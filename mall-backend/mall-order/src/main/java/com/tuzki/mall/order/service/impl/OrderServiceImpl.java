package com.tuzki.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 订单业务默认实现，编排用户、地址、商品、库存和订单明细完成下单流程。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int PENDING_PAYMENT_STATUS = 10;

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
        // 校验基础信息
        User user = getActiveUser(request.getUserId());
        Address address = getActiveAddress(user.getId(), request.getAddressId());
        Sku sku = getActiveSku(request.getSkuId());
        Product product = getActiveProduct(sku.getProductId());

        BigDecimal totalAmount = sku.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        String orderNo = generateOrderNo();

        // 锁库存、创建订单、创建订单明细必须在同一个事务中完成，避免库存已锁定但订单创建失败。
        inventoryService.lockStock(sku.getId(), request.getQuantity());

        Order order = buildOrder(request, address, totalAmount, orderNo);
        orderMapper.insert(order);

        OrderItem orderItem = buildOrderItem(request, sku, product, order);
        orderItemMapper.insert(orderItem);

        return toCreateVO(order);
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

    private Order buildOrder(OrderCreateRequest request, Address address, BigDecimal totalAmount, String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setFreightAmount(ZERO_FREIGHT_AMOUNT);
        order.setStatus(PENDING_PAYMENT_STATUS);
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

    private OrderItem buildOrderItem(OrderCreateRequest request, Sku sku, Product product, Order order) {
        BigDecimal totalAmount = sku.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
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
        orderItem.setQuantity(request.getQuantity());
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

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "O" + timestamp + randomSuffix;
    }
}
