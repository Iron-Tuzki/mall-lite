package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeMessageSender;
import com.tuzki.mall.config.rabbit.CartRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.springframework.stereotype.Component;

/**
 * 购物车 Outbox 变更消息发送器，先记录本地消息，再尝试投递 RabbitMQ。
 */
@Component
public class CartOutboxChangeMessageSender implements CartChangeMessageSender {

    private static final String AGGREGATE_TYPE = "CART";

    private final OutboxMessageService outboxMessageService;

    private final CartRabbitProperties properties;

    public CartOutboxChangeMessageSender(OutboxMessageService outboxMessageService,
                                         CartRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    /**
     * 写入购物车变更 Outbox 消息，并立即尝试投递一次 RabbitMQ。
     *
     * @param message 购物车变更消息
     */
    @Override
    public void send(CartChangeMessage message) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                aggregateId(message),
                properties.getChangeExchange(),
                properties.getChangeRoutingKey(),
                message
        );
    }

    private String aggregateId(CartChangeMessage message) {
        return message.userId() + ":" + message.skuId() + ":" + message.version();
    }
}
