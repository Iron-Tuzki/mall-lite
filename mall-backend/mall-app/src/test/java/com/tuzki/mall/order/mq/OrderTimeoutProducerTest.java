package com.tuzki.mall.order.mq;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 订单超时消息生产者测试，验证消息会被投递到配置指定的延迟交换机和路由键。
 */
class OrderTimeoutProducerTest {

    @Test
    void sendUsesDelayExchangeAndRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderRabbitProperties properties = new OrderRabbitProperties();
        properties.setDelayExchange("mall.order.delay.exchange");
        properties.setDelayRoutingKey("mall.order.delay.routing-key");
        OrderTimeoutProducer producer = new OrderTimeoutProducer(rabbitTemplate, properties);
        OrderTimeoutMessage message = buildMessage();

        producer.send(message);

        verify(rabbitTemplate).convertAndSend(
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
