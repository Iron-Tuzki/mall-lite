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
import com.tuzki.mall.payment.enums.PayChannel;
import com.tuzki.mall.payment.enums.PaymentStatus;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 支付事务服务，负责在同一事务中写入支付流水、更新订单状态并扣减锁定库存。
 */
@Service
public class PaymentTransactionService {

    private static final int NOT_DELETED = 0;

    private final OrderMapper orderMapper;

    private final OrderItemMapper orderItemMapper;

    private final PaymentMapper paymentMapper;

    private final InventoryService inventoryService;

    public PaymentTransactionService(OrderMapper orderMapper,
                                     OrderItemMapper orderItemMapper,
                                     PaymentMapper paymentMapper,
                                     InventoryService inventoryService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.inventoryService = inventoryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentPayVO confirmPaymentSuccess(Long orderId) {
        // 1.校验订单状态
        Order order = getActiveOrder(orderId);
        OrderStatus.fromCode(order.getStatus()).checkCanPay();
        // 2.查询订单明细
        List<OrderItem> orderItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId())
                .eq(OrderItem::getDeleted, NOT_DELETED));
        if (orderItems.isEmpty()) {
            throw new BusinessException(404, "order item not found");
        }
        // 3.生成支付流水
        LocalDateTime payTime = LocalDateTime.now();
        Payment payment = buildPayment(order, payTime);
        paymentMapper.insert(payment);
        // 4.按订单明细分别扣减锁定库存
        for (OrderItem orderItem : orderItems) {
            inventoryService.deductLockedStock(orderItem.getSkuId(), orderItem.getQuantity());
        }
        // 5.更改订单状态
        order.setStatus(OrderStatus.PAID.getCode());
        order.setPayTime(payTime);
        orderMapper.updateById(order);

        return toPaymentPayVO(payment, order);
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

    private Payment buildPayment(Order order, LocalDateTime payTime) {
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setPayChannel(PayChannel.MOCK.getCode());
        payment.setPayAmount(order.getPayAmount());
        payment.setStatus(PaymentStatus.SUCCESS.getCode());
        payment.setPayTime(payTime);
        payment.setCallbackContent("{\"channel\":\"mock\",\"result\":\"success\"}");
        payment.setDeleted(NOT_DELETED);
        return payment;
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
}
