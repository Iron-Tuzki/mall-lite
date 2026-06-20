package com.tuzki.mall.order.mq;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import com.tuzki.mall.order.message.OrderTimeoutMessage;
import com.tuzki.mall.order.message.OrderTimeoutMessageSender;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.springframework.stereotype.Component;

/**
 * 订单超时消息发送器，负责将订单超时检查消息写入 Outbox 并投递到 RabbitMQ 延迟交换机。
 */
@Component
public class OrderTimeoutSender implements OrderTimeoutMessageSender {

    private static final String AGGREGATE_TYPE = "ORDER_TIMEOUT";

    private final OutboxMessageService outboxMessageService;

    private final OrderRabbitProperties properties;

    public OrderTimeoutSender(OutboxMessageService outboxMessageService, OrderRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    /**
     * 发送订单超时检查消息。
     *
     * @param message 订单超时检查消息，包含订单 ID、订单号等消费者重新查询订单所需的关键字段
     */
    @Override
    public void send(OrderTimeoutMessage message) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                String.valueOf(message.getOrderId()),
                properties.getDelayExchange(),
                properties.getDelayRoutingKey(),
                message
        );
    }
}
