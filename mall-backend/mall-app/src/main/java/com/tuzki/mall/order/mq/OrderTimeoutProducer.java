package com.tuzki.mall.order.mq;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.message.OrderTimeoutMessageSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息生产者，负责把订单超时检查消息投递到 RabbitMQ 延迟交换机。
 */
@Component
public class OrderTimeoutProducer implements OrderTimeoutMessageSender {

    private final RabbitTemplate rabbitTemplate;

    private final OrderRabbitProperties properties;

    public OrderTimeoutProducer(RabbitTemplate rabbitTemplate, OrderRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * 发送订单超时检查消息。
     *
     * @param message 订单超时检查消息，包含订单 ID、订单号等消费者重新查询订单所需的关键字段
     */
    @Override
    public void send(OrderTimeoutMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getDelayExchange(),
                properties.getDelayRoutingKey(),
                message
        );
    }
}
