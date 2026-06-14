package com.tuzki.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.order.dto.OrderCreateItemRequest;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.entity.OrderRequest;
import com.tuzki.mall.order.enums.OrderCancelType;
import com.tuzki.mall.order.enums.OrderRequestStatus;
import com.tuzki.mall.order.enums.OrderStatus;
import com.tuzki.mall.order.event.OrderCancelledEvent;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.order.mapper.OrderRequestMapper;
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.message.OrderTimeoutMessageSender;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.order.vo.OrderDetailVO;
import com.tuzki.mall.order.vo.OrderItemVO;
import com.tuzki.mall.order.vo.OrderMainVO;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.user.entity.Address;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.AddressMapper;
import com.tuzki.mall.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 订单业务默认实现，编排用户、地址、商品、库存和订单明细完成下单、查询和取消流程。
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final BigDecimal ZERO_FREIGHT_AMOUNT = BigDecimal.ZERO;

    private static final int DUPLICATED_REQUEST_RETRY_TIMES = 20;

    private static final long DUPLICATED_REQUEST_RETRY_INTERVAL_MILLIS = 100L;

    private final UserMapper userMapper;

    private final AddressMapper addressMapper;

    private final SkuMapper skuMapper;

    private final ProductMapper productMapper;

    private final InventoryService inventoryService;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderRequestMapper orderRequestMapper;

    private final OrderTimeoutMessageSender orderTimeoutMessageSender;

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderServiceImpl(UserMapper userMapper,
                            AddressMapper addressMapper,
                            SkuMapper skuMapper,
                            ProductMapper productMapper,
                            InventoryService inventoryService,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            OrderRequestMapper orderRequestMapper,
                            OrderTimeoutMessageSender orderTimeoutMessageSender,
                            ApplicationEventPublisher applicationEventPublisher) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderRequestMapper = orderRequestMapper;
        this.orderTimeoutMessageSender = orderTimeoutMessageSender;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrderCreateVO createOrder(Long userId, OrderCreateRequest request) {
        return createOrderInternal(userId, request, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrderCreateVO createOrderWithPriceOverrides(Long userId,
                                                       OrderCreateRequest request,
                                                       Map<Long, BigDecimal> priceOverrides) {
        if (priceOverrides == null || priceOverrides.isEmpty()) {
            throw new BusinessException(400, "price overrides must not be empty");
        }
        return createOrderInternal(userId, request, priceOverrides);
    }

    private OrderCreateVO createOrderInternal(Long userId,
                                             OrderCreateRequest request,
                                             Map<Long, BigDecimal> priceOverrides) {
        // todo 此处查询是否可以删除
        Order existingOrder = getExistingOrderByRequestId(userId, request.getRequestId());
        if (existingOrder != null) {
            return toCreateVO(existingOrder);
        }
        if (!claimOrderRequest(userId, request.getRequestId())) {
            // sugus：如果没有抢到幂等，则多次重试查询数据库后返回
            return toCreateVO(getExistingOrderAfterDuplicatedRequest(userId, request.getRequestId()));
        }

        User user = getActiveUser(userId);
        Address address = getActiveAddress(user.getId(), request.getAddressId());
        List<OrderItemContext> itemContexts = buildItemContexts(request.getItems());
        BigDecimal totalAmount = calculateTotalAmount(itemContexts, priceOverrides);

        List<OrderItemContext> lockedItemContexts = new ArrayList<>();
        for (OrderItemContext itemContext : itemContexts) {
            inventoryService.lockStock(itemContext.sku().getId(), itemContext.requestItem().getQuantity());
            lockedItemContexts.add(itemContext);
        }

        Order order = buildOrder(request, address, totalAmount, generateOrderNo());
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            Order concurrentExistingOrder = getExistingOrderByRequestId(userId, request.getRequestId());
            if (concurrentExistingOrder != null) {
                // 兼容历史数据或极端并发场景：订单唯一键仍作为最终兜底，避免重复订单落库。
                releaseLockedStock(lockedItemContexts);
                return toCreateVO(concurrentExistingOrder);
            }
            throw exception;
        }

        for (OrderItemContext itemContext : itemContexts) {
            OrderItem orderItem = buildOrderItem(
                    itemContext.requestItem(),
                    itemContext.sku(),
                    itemContext.product(),
                    order,
                    priceOverrides);
            orderItemMapper.insert(orderItem);
        }
        markOrderRequestSuccess(userId, request.getRequestId(), order.getId());
        // sugus:订单事务提交后，发送定时取消的消息
        registerOrderTimeoutMessageAfterCommit(order);

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
        // 先判断一次状态
        Order order = getActiveOrder(orderId);
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        if (status == OrderStatus.CANCELLED) {
            return;
        }
        status.checkCanCancel();

        if (!cancelPendingOrderAndReleaseStock(order, OrderCancelType.USER_CANCEL)) {
            handleManualCancelConflict(orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        Order order = getActiveOrderOrNull(orderId);
        if (order == null || OrderStatus.fromCode(order.getStatus()) != OrderStatus.PENDING_PAYMENT) {
            return;
        }

        cancelPendingOrderAndReleaseStock(order, OrderCancelType.TIMEOUT_CANCEL);
    }

    @Override
    public List<Long> listTimeoutPendingOrderIds(LocalDateTime timeoutBefore, Integer batchSize) {
        return orderMapper.listTimeoutPendingOrderIds(timeoutBefore, batchSize);
    }

    @Override
    public List<OrderMainVO> listOrders(Long userId, Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        return orderMapper.listOrders(userId, status, startTime, endTime);
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

    private Order getActiveOrderOrNull(Long orderId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getDeleted, NOT_DELETED));
    }

    private Order getActiveOrderForUpdate(Long orderId) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
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

    private boolean claimOrderRequest(Long userId, String requestId) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setRequestId(requestId);
        orderRequest.setStatus(OrderRequestStatus.PROCESSING.getCode());
        orderRequest.setDeleted(NOT_DELETED);
        try {
            orderRequestMapper.insert(orderRequest);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private Order getExistingOrderAfterDuplicatedRequest(Long userId, String requestId) {
        for (int retry = 0; retry < DUPLICATED_REQUEST_RETRY_TIMES; retry++) {
            Order existingOrder = getExistingOrderByRequestId(userId, requestId);
            if (existingOrder != null) {
                return existingOrder;
            }

            waitForDuplicatedRequestResult();
        }
        throw new BusinessException(409, "order request is processing");
    }

    private void waitForDuplicatedRequestResult() {
        try {
            Thread.sleep(DUPLICATED_REQUEST_RETRY_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(409, "order request is processing");
        }
    }

    private void markOrderRequestSuccess(Long userId, String requestId, Long orderId) {
        int affectedRows = orderRequestMapper.markSuccess(userId, requestId, orderId);
        if (affectedRows != 1) {
            throw new BusinessException(500, "mark order request success failed");
        }
    }

    private void registerOrderTimeoutMessageAfterCommit(Order order) {
        OrderTimeoutMessage message = buildOrderTimeoutMessage(order);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    orderTimeoutMessageSender.send(message);
                } catch (RuntimeException exception) {
                    LOGGER.warn("send order timeout message failed, orderId={}", order.getId(), exception);
                }
            }
        });
    }

    private OrderTimeoutMessage buildOrderTimeoutMessage(Order order) {
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderId(order.getId());
        message.setOrderNo(order.getOrderNo());
        message.setUserId(order.getUserId());
        message.setCreateTime(order.getCreateTime() == null ? LocalDateTime.now() : order.getCreateTime());
        return message;
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

    private BigDecimal calculateTotalAmount(List<OrderItemContext> itemContexts, Map<Long, BigDecimal> priceOverrides) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemContext itemContext : itemContexts) {
            BigDecimal itemAmount = resolveUnitPrice(itemContext.sku(), priceOverrides)
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

    private boolean cancelPendingOrderAndReleaseStock(Order order, OrderCancelType cancelType) {
        // 只有抢到 PENDING_PAYMENT -> CANCELLED 状态流转的线程，才允许释放库存，避免并发重复释放。
        int affectedRows = orderMapper.markCancelIfPending(order.getId(), cancelType.getCode(), cancelType.getReason());
        if (affectedRows == 0) {
            return false;
        }

        List<OrderItem> orderItems = getActiveOrderItems(order.getId());
        for (OrderItem orderItem : orderItems) {
            inventoryService.releaseStock(orderItem.getSkuId(), orderItem.getQuantity());
        }
        applicationEventPublisher.publishEvent(new OrderCancelledEvent(order.getId(), cancelType));
        return true;
    }

    private void handleManualCancelConflict(Long orderId) {
        // CAS 失败后用 FOR UPDATE 当前读重新确认状态，避免 RR 隔离级别下普通快照读看不到并发提交。
        Order latestOrder = getActiveOrderForUpdate(orderId);
        OrderStatus latestStatus = OrderStatus.fromCode(latestOrder.getStatus());
        if (latestStatus == OrderStatus.CANCELLED) {
            return;
        }
        latestStatus.checkCanCancel();
        throw new BusinessException(409, "order cancellation is processing");
    }

    private Order buildOrder(OrderCreateRequest request, Address address, BigDecimal totalAmount, String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setRequestId(request.getRequestId());
        order.setUserId(address.getUserId());
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

    private OrderItem buildOrderItem(OrderCreateItemRequest requestItem,
                                     Sku sku,
                                     Product product,
                                     Order order,
                                     Map<Long, BigDecimal> priceOverrides) {
        BigDecimal unitPrice = resolveUnitPrice(sku, priceOverrides);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(requestItem.getQuantity()));
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
        orderItem.setUnitPrice(unitPrice);
        orderItem.setQuantity(requestItem.getQuantity());
        orderItem.setTotalAmount(totalAmount);
        orderItem.setDeleted(NOT_DELETED);
        return orderItem;
    }

    private BigDecimal resolveUnitPrice(Sku sku, Map<Long, BigDecimal> priceOverrides) {
        if (priceOverrides.isEmpty()) {
            return sku.getPrice();
        }
        BigDecimal overridePrice = priceOverrides.get(sku.getId());
        if (overridePrice == null) {
            throw new BusinessException(400, "override price not found");
        }
        if (overridePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "override price must be greater than 0");
        }
        return overridePrice;
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
        orderDetailVO.setCancelType(order.getCancelType());
        orderDetailVO.setCancelReason(order.getCancelReason());
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
