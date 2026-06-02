package com.tuzki.mall.order.scheduling;

import com.tuzki.mall.order.service.OrderService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单超时补偿任务测试，验证任务按配置批次扫描并复用订单取消能力。
 */
class OrderTimeoutCompensationTaskTest {

    @Test
    void cancelsScannedTimeoutOrders() {
        OrderService orderService = mock(OrderService.class);
        OrderSchedulingProperties properties = new OrderSchedulingProperties();
        properties.setTimeoutMinutes(30);
        properties.setBatchSize(100);
        when(orderService.listTimeoutPendingOrderIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(1001L, 1002L));

        new OrderTimeoutCompensationTask(orderService, properties).cancelTimeoutOrders();

        verify(orderService).cancelTimeoutOrder(1001L);
        verify(orderService).cancelTimeoutOrder(1002L);
    }

    @Test
    void continuesCompensatingWhenOneOrderFails() {
        OrderService orderService = mock(OrderService.class);
        OrderSchedulingProperties properties = new OrderSchedulingProperties();
        when(orderService.listTimeoutPendingOrderIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(List.of(1001L, 1002L));
        doThrow(new IllegalStateException("temporary database error"))
                .when(orderService).cancelTimeoutOrder(1001L);

        new OrderTimeoutCompensationTask(orderService, properties).cancelTimeoutOrders();

        verify(orderService).cancelTimeoutOrder(1002L);
    }
}
