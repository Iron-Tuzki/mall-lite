package com.tuzki.mall.payment;

import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.entity.OrderItem;
import com.tuzki.mall.order.enums.OrderStatus;
import com.tuzki.mall.order.mapper.OrderItemMapper;
import com.tuzki.mall.order.mapper.OrderMapper;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.enums.MockPaymentResult;
import com.tuzki.mall.payment.enums.PaymentStatus;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import com.tuzki.mall.payment.service.impl.PaymentTransactionService;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 支付事务服务热点事件测试，验证支付成功后按商品维度发送热点事件。
 */
class PaymentTransactionServiceHotEventTest {

    @Test
    void successfulPaymentCallbackSendsOneHotEventPerProduct() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
        PaymentMapper paymentMapper = mock(PaymentMapper.class);
        ProductHotEventSender productHotEventSender = mock(ProductHotEventSender.class);
        when(paymentMapper.selectOne(any())).thenReturn(pendingPayment());
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(pendingOrder());
        when(paymentMapper.markSuccessIfPending(any(), any(), any())).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(
                orderItem(100L, 10L),
                orderItem(100L, 11L)
        ));
        when(orderMapper.markPaidIfPending(any(), any())).thenReturn(1);

        new PaymentTransactionService(
                orderMapper,
                orderItemMapper,
                paymentMapper,
                mock(InventoryService.class),
                productHotEventSender)
                .handleCallback("P100", MockPaymentResult.SUCCESS);

        verify(productHotEventSender, times(1)).send(argThat(event ->
                event.productId().equals(100L) && event.action() == ProductHotAction.PAY_SUCCESS));
    }

    private Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setPaymentNo("P100");
        payment.setOrderId(1L);
        payment.setOrderNo("O100");
        payment.setUserId(1L);
        payment.setPayAmount(new BigDecimal("99.90"));
        payment.setStatus(PaymentStatus.PENDING.getCode());
        payment.setDeleted(0);
        return payment;
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("O100");
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setPayAmount(new BigDecimal("99.90"));
        order.setDeleted(0);
        return order;
    }

    private OrderItem orderItem(Long productId, Long skuId) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(productId);
        orderItem.setSkuId(skuId);
        orderItem.setQuantity(1);
        orderItem.setDeleted(0);
        return orderItem;
    }
}
