package com.tuzki.mall.product.mq;

import com.tuzki.mall.config.rabbit.ProductHotRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import org.springframework.stereotype.Component;

/**
 * 商品热点事件发送器，负责将热点事件写入 Outbox 并投递到 RabbitMQ。
 */
@Component
public class ProductHotSender implements ProductHotEventSender {

    private static final String AGGREGATE_TYPE = "PRODUCT_HOT";

    private final OutboxMessageService outboxMessageService;

    private final ProductHotRabbitProperties properties;

    public ProductHotSender(OutboxMessageService outboxMessageService, ProductHotRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    /**
     * 发送商品热点事件消息。
     *
     * @param event 商品热点事件，包含事件 ID、商品 ID、行为类型和发生时间
     */
    @Override
    public void send(ProductHotEvent event) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                event.eventId(),
                properties.getEventExchange(),
                properties.getEventRoutingKey(),
                event
        );
    }
}
