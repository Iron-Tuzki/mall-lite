package com.tuzki.mall.order.mq;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 订单超时消息发送器测试，验证订单超时消息会先写入 Outbox 再投递延迟队列。
 */
class OrderTimeoutSenderTest {

    @Test
    void sendCreatesOutboxMessageUsingDelayExchangeAndRoutingKey() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        OrderRabbitProperties properties = new OrderRabbitProperties();
        properties.setDelayExchange("mall.order.delay.exchange");
        properties.setDelayRoutingKey("mall.order.delay.routing-key");
        OrderTimeoutMessage message = buildMessage();

        new OrderTimeoutSender(outboxMessageService, properties).send(message);

        verify(outboxMessageService).createAndPublish(
                "ORDER_TIMEOUT",
                "1001",
                "mall.order.delay.exchange",
                "mall.order.delay.routing-key",
                message
        );
    }

    private OrderTimeoutMessage buildMessage() {
        OrderTimeoutMessage message = new OrderTimeoutMessage();
        message.setOrderId(1001L);
        message.setOrderNo("O202605261001");
        message.setUserId(2001L);
        message.setCreateTime(LocalDateTime.of(2026, 5, 26, 19, 50));
        return message;
    }
}
