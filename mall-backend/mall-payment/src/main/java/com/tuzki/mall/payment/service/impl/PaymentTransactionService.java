package com.tuzki.mall.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.enums.OrderStatus;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.enums.MockPaymentResult;
import com.tuzki.mall.payment.enums.PayChannel;
import com.tuzki.mall.payment.enums.PaymentStatus;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 支付事务服务，负责在本地事务中创建支付流水、处理回调、更新订单状态并确认扣减锁定库存。
 */
@Service
public class PaymentTransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentTransactionService.class);

    private static final int NOT_DELETED = 0;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final PaymentMapper paymentMapper;

    private final InventoryService inventoryService;

    private final ProductHotEventSender productHotEventSender;

    public PaymentTransactionService(OrderMapper orderMapper,
                                     OrderItemMapper orderItemMapper,
                                     PaymentMapper paymentMapper,
                                     InventoryService inventoryService,
                                     ProductHotEventSender productHotEventSender) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.inventoryService = inventoryService;
        this.productHotEventSender = productHotEventSender;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentPayVO createPendingPayment(Long orderId) {
        // 先锁定订单行（和下单接口保持相同的加锁顺序）。
        // 如果有重复请求，因为加锁所以串行化
        Order order = getActiveOrderForUpdate(orderId);
        OrderStatus.fromCode(order.getStatus()).checkCanPay();

        Payment payment = buildPendingPayment(order);
        try {
            paymentMapper.insert(payment);
            return toPaymentPayVO(payment, order);
        } catch (DuplicateKeyException exception) {
            // 使用当前读查询其他事务已经提交的数据，避免依赖 Read View 的创建时机。
            Payment existingPendingPayment = paymentMapper.selectPendingByOrderIdForUpdate(order.getId());
            if (existingPendingPayment != null) {
                return toPaymentPayVO(existingPendingPayment, order);
            }
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentPayVO handleCallback(String paymentNo, MockPaymentResult mockResult) {
        Payment payment = getActivePayment(paymentNo);
        Order order = getActiveOrderForUpdate(payment.getOrderId());
        LocalDateTime now = LocalDateTime.now();
        // 使用状态机实现支付回调幂等
        int affectedRows = markPaymentTerminalIfPending(paymentNo, now, mockResult);
        // affectedRows 为 0 表示支付流水已被其他回调处理，使用当前读读取最新数据并返回，避免重复扣库存。
        if (affectedRows == 0) {
            Payment currentPayment = getActivePaymentForUpdate(paymentNo);
            return toPaymentPayVO(currentPayment, order);
        }

        payment.setStatus(mockResult == MockPaymentResult.SUCCESS
                ? PaymentStatus.SUCCESS.getCode()
                : PaymentStatus.FAILED.getCode());

        if (mockResult == MockPaymentResult.SUCCESS) {
            payment.setPayTime(now);
            List<Long> paidProductIds = confirmPaymentSuccess(payment, order);
            // 支付成功提交后发送热点事件
            runAfterCommit(() -> sendPaymentHotEventsQuietly(paidProductIds));
        }
        return toPaymentPayVO(payment, order);
    }

    private int markPaymentTerminalIfPending(String paymentNo, LocalDateTime now, MockPaymentResult mockResult) {
        String callbackContent = buildCallbackContent(mockResult);
        if (mockResult == MockPaymentResult.SUCCESS) {
            return paymentMapper.markSuccessIfPending(paymentNo, now, callbackContent);
        }
        return paymentMapper.markFailedIfPending(paymentNo, callbackContent);
    }

    private List<Long> confirmPaymentSuccess(Payment payment, Order order) {
        OrderStatus.fromCode(order.getStatus()).checkCanPay();

        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getDeleted, NOT_DELETED));
        if (orderItems.isEmpty()) {
            throw new BusinessException(404, "order item not found");
        }

        for (OrderItem orderItem : orderItems) {
            inventoryService.deductLockedStock(orderItem.getSkuId(), orderItem.getQuantity());
        }

        int affectedRows = orderMapper.markPaidIfPending(order.getId(), payment.getPayTime());
        if (affectedRows != 1) {
            throw new BusinessException(400, "mark order paid failed");
        }
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(payment.getPayTime());
        return distinctProductIds(orderItems);
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

    private Order getActiveOrderForUpdate(Long orderId) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(404, "order not found");
        }
        return order;
    }

    private Payment getActivePayment(String paymentNo) {
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getDeleted, NOT_DELETED));
        if (payment == null) {
            throw new BusinessException(404, "payment not found");
        }
        return payment;
    }

    private Payment getActivePaymentForUpdate(String paymentNo) {
        Payment payment = paymentMapper.selectByPaymentNoForUpdate(paymentNo);
        if (payment == null) {
            throw new BusinessException(404, "payment not found");
        }
        return payment;
    }

    private Payment buildPendingPayment(Order order) {
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setPayChannel(PayChannel.MOCK.getCode());
        payment.setPayAmount(order.getPayAmount());
        payment.setStatus(PaymentStatus.PENDING.getCode());
        payment.setDeleted(NOT_DELETED);
        return payment;
    }

    private String buildCallbackContent(MockPaymentResult mockResult) {
        return "{\"channel\":\"mock\",\"result\":\"" + mockResult.name().toLowerCase() + "\"}";
    }

    private PaymentPayVO toPaymentPayVO(Payment payment, Order order) {
        PaymentPayVO paymentPayVO = new PaymentPayVO();
        paymentPayVO.setPaymentNo(payment.getPaymentNo());
        paymentPayVO.setOrderId(order.getId());
        paymentPayVO.setOrderNo(order.getOrderNo());
        paymentPayVO.setOrderStatus(order.getStatus());
        paymentPayVO.setPaymentStatus(payment.getStatus());
        paymentPayVO.setPayAmount(payment.getPayAmount());
        return paymentPayVO;
    }

    private String generatePaymentNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "P" + timestamp + randomSuffix;
    }

    private List<Long> distinctProductIds(List<OrderItem> orderItems) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getProductId() != null) {
                productIds.add(orderItem.getProductId());
            }
        }
        return productIds.stream().toList();
    }

    private void sendPaymentHotEventsQuietly(List<Long> productIds) {
        for (Long productId : productIds) {
            try {
                productHotEventSender.send(new ProductHotEvent(
                        UUID.randomUUID().toString(),
                        productId,
                        ProductHotAction.PAY_SUCCESS,
                        LocalDateTime.now()));
            } catch (RuntimeException exception) {
                LOGGER.warn("send payment product hot event failed, productId={}", productId, exception);
            }
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 没有事务，不存在事务提交，代码没法延迟执行，立刻运行传入的任务。
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
