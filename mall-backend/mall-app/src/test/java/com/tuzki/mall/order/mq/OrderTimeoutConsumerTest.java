package com.tuzki.mall.order.mq;

import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.service.OrderService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 订单超时消息消费者测试，验证消费者收到消息后会触发订单超时取消业务。
 */
class OrderTimeoutConsumerTest {

    @Test
    void handleCancelsTimeoutOrder() {
        OrderService orderService = mock(OrderService.class);
        OrderTimeoutConsumer consumer = new OrderTimeoutConsumer(orderService);
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderId(1001L);

        consumer.handle(message);

        verify(orderService).cancelTimeoutOrder(1001L);
    }
}
