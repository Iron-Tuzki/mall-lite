package com.tuzki.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.order.dto.OrderCreateItemRequest;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.enums.OrderStatus;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.order.vo.OrderDetailVO;
import com.tuzki.mall.order.vo.OrderItemVO;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 订单业务默认实现，编排用户、地址、商品、库存和订单明细完成下单、查询和取消流程。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final BigDecimal ZERO_FREIGHT_AMOUNT = BigDecimal.ZERO;

    private final UserMapper userMapper;

    private final AddressMapper addressMapper;

    private final SkuMapper skuMapper;

    private final ProductMapper productMapper;

    private final InventoryService inventoryService;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    public OrderServiceImpl(UserMapper userMapper,
                            AddressMapper addressMapper,
                            SkuMapper skuMapper,
                            ProductMapper productMapper,
                            InventoryService inventoryService,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(OrderCreateRequest request) {
        Order existingOrder = getExistingOrderByRequestId(request.getUserId(), request.getRequestId());
        if (existingOrder != null) {
            return toCreateVO(existingOrder);
        }

        User user = getActiveUser(request.getUserId());
        Address address = getActiveAddress(user.getId(), request.getAddressId());
        List<OrderItemContext> itemContexts = buildItemContexts(request.getItems());
        BigDecimal totalAmount = calculateTotalAmount(itemContexts);

        List<OrderItemContext> lockedItemContexts = new ArrayList<>();
        for (OrderItemContext itemContext : itemContexts) {
            inventoryService.lockStock(itemContext.sku().getId(), itemContext.requestItem().getQuantity());
            lockedItemContexts.add(itemContext);
        }

        Order order = buildOrder(request, address, totalAmount, generateOrderNo());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            Order concurrentExistingOrder = getExistingOrderByRequestId(request.getUserId(), request.getRequestId());
            if (concurrentExistingOrder != null) {
                // 并发重复请求可能已经由另一个线程建单成功，本线程释放刚锁定的多条库存后返回已有订单。
                releaseLockedStock(lockedItemContexts);
                return toCreateVO(concurrentExistingOrder);
            }
            throw exception;
        }

        for (OrderItemContext itemContext : itemContexts) {
            OrderItem orderItem = buildOrderItem(itemContext.requestItem(), itemContext.sku(), itemContext.product(), order);
            orderItemMapper.insert(orderItem);
        }

        return toCreateVO(order);
    }

    @Override
    public OrderDetailVO getOrderById(Long orderId) {
        Order order = getActiveOrder(orderId);
        List<OrderItem> orderItems = getActiveOrderItems(order.getId());
        return toDetailVO(order, orderItems);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = getActiveOrder(orderId);
        OrderStatus.fromCode(order.getStatus()).checkCanCancel();

        List<OrderItem> orderItems = getActiveOrderItems(order.getId());
        for (OrderItem orderItem : orderItems) {
            inventoryService.releaseStock(orderItem.getSkuId(), orderItem.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private User getActiveUser(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getStatus, ACTIVE_STATUS)
                .eq(User::getDeleted, NOT_DELETED));
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        return user;
    }

    private Address getActiveAddress(Long userId, Long addressId) {
        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .eq(Address::getDeleted, NOT_DELETED));
        if (address == null) {
            throw new BusinessException(404, "address not found");
        }
        return address;
    }

    private Sku getActiveSku(Long skuId) {
        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        return sku;
    }

    private Product getActiveProduct(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED));
        if (product == null) {
            throw new BusinessException(404, "product not found");
        }
        return product;
    }

    private Order getActiveOrder(Long orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getDeleted, NOT_DELETED));
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        return order;
    }

    private Order getExistingOrderByRequestId(Long userId, String requestId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, requestId)
                .eq(Order::getDeleted, NOT_DELETED)
                .last("limit 1"));
    }

    private List<OrderItem> getActiveOrderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getDeleted, NOT_DELETED));
    }

    private List<OrderItemContext> buildItemContexts(List<OrderCreateItemRequest> requestItems) {
        Set<Long> skuIds = new HashSet<>();
        List<OrderItemContext> itemContexts = new ArrayList<>();
        for (OrderCreateItemRequest requestItem : requestItems) {
            if (!skuIds.add(requestItem.getSkuId())) {
                throw new BusinessException(400, "duplicated sku in order items");
            }
            Sku sku = getActiveSku(requestItem.getSkuId());
            Product product = getActiveProduct(sku.getProductId());
            itemContexts.add(new OrderItemContext(requestItem, sku, product));
        }
        return itemContexts;
    }

    private BigDecimal calculateTotalAmount(List<OrderItemContext> itemContexts) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemContext itemContext : itemContexts) {
            BigDecimal itemAmount = itemContext.sku().getPrice()
                    .multiply(BigDecimal.valueOf(itemContext.requestItem().getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
        }
        return totalAmount;
    }

    private void releaseLockedStock(List<OrderItemContext> lockedItemContexts) {
        for (OrderItemContext itemContext : lockedItemContexts) {
            inventoryService.releaseStock(itemContext.sku().getId(), itemContext.requestItem().getQuantity());
        }
    }

    private Order buildOrder(OrderCreateRequest request, Address address, BigDecimal totalAmount, String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setRequestId(request.getRequestId());
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setFreightAmount(ZERO_FREIGHT_AMOUNT);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetailAddress(address.getDetailAddress());
        order.setRemark(request.getRemark());
        order.setDeleted(NOT_DELETED);
        return order;
    }

    private OrderItem buildOrderItem(OrderCreateItemRequest requestItem, Sku sku, Product product, Order order) {
        BigDecimal totalAmount = sku.getPrice().multiply(BigDecimal.valueOf(requestItem.getQuantity()));
        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setOrderNo(order.getOrderNo());
        orderItem.setProductId(product.getId());
        orderItem.setSkuId(sku.getId());
        orderItem.setSkuCode(sku.getSkuCode());
        orderItem.setProductName(product.getName());
        orderItem.setSkuName(sku.getSkuName());
        orderItem.setSpecData(sku.getSpecData());
        orderItem.setMainImageUrl(resolveMainImageUrl(sku, product));
        orderItem.setUnitPrice(sku.getPrice());
        orderItem.setQuantity(requestItem.getQuantity());
        orderItem.setTotalAmount(totalAmount);
        orderItem.setDeleted(NOT_DELETED);
        return orderItem;
    }

    private String resolveMainImageUrl(Sku sku, Product product) {
        if (sku.getMainImageUrl() != null && !sku.getMainImageUrl().isBlank()) {
            return sku.getMainImageUrl();
        }
        return product.getMainImageUrl();
    }

    private OrderCreateVO toCreateVO(Order order) {
        OrderCreateVO orderCreateVO = new OrderCreateVO();
        orderCreateVO.setOrderId(order.getId());
        orderCreateVO.setOrderNo(order.getOrderNo());
        orderCreateVO.setTotalAmount(order.getTotalAmount());
        orderCreateVO.setPayAmount(order.getPayAmount());
        orderCreateVO.setStatus(order.getStatus());
        return orderCreateVO;
    }

    private OrderDetailVO toDetailVO(Order order, List<OrderItem> orderItems) {
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setOrderId(order.getId());
        orderDetailVO.setOrderNo(order.getOrderNo());
        orderDetailVO.setUserId(order.getUserId());
        orderDetailVO.setTotalAmount(order.getTotalAmount());
        orderDetailVO.setPayAmount(order.getPayAmount());
        orderDetailVO.setFreightAmount(order.getFreightAmount());
        orderDetailVO.setStatus(order.getStatus());
        orderDetailVO.setReceiverName(order.getReceiverName());
        orderDetailVO.setReceiverPhone(order.getReceiverPhone());
        orderDetailVO.setReceiverProvince(order.getReceiverProvince());
        orderDetailVO.setReceiverCity(order.getReceiverCity());
        orderDetailVO.setReceiverDistrict(order.getReceiverDistrict());
        orderDetailVO.setReceiverDetailAddress(order.getReceiverDetailAddress());
        orderDetailVO.setRemark(order.getRemark());
        orderDetailVO.setCreateTime(order.getCreateTime());
        orderDetailVO.setItems(orderItems.stream()
                .map(this::toItemVO)
                .toList());
        return orderDetailVO;
    }

    private OrderItemVO toItemVO(OrderItem orderItem) {
        OrderItemVO orderItemVO = new OrderItemVO();
        orderItemVO.setId(orderItem.getId());
        orderItemVO.setSkuId(orderItem.getSkuId());
        orderItemVO.setSkuCode(orderItem.getSkuCode());
        orderItemVO.setProductName(orderItem.getProductName());
        orderItemVO.setSkuName(orderItem.getSkuName());
        orderItemVO.setSpecData(orderItem.getSpecData());
        orderItemVO.setMainImageUrl(orderItem.getMainImageUrl());
        orderItemVO.setUnitPrice(orderItem.getUnitPrice());
        orderItemVO.setQuantity(orderItem.getQuantity());
        orderItemVO.setTotalAmount(orderItem.getTotalAmount());
        return orderItemVO;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "O" + timestamp + randomSuffix;
    }

    private record OrderItemContext(OrderCreateItemRequest requestItem, Sku sku, Product product) {
    }
}
