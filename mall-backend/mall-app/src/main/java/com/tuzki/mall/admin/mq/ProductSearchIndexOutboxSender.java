package com.tuzki.mall.admin.mq;

import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEvent;
import com.tuzki.mall.admin.product.search.ProductSearchIndexEventSender;
import com.tuzki.mall.config.rabbit.ProductSearchIndexRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.springframework.stereotype.Component;

/**
 * 商品搜索索引 Outbox 事件发送器，负责将商品搜索索引变更事件写入本地消息表并投递 RabbitMQ。
 */
@Component
public class ProductSearchIndexOutboxSender implements ProductSearchIndexEventSender {

    private static final String AGGREGATE_TYPE = "PRODUCT_SEARCH_INDEX";

    private final OutboxMessageService outboxMessageService;

    private final ProductSearchIndexRabbitProperties properties;

    public ProductSearchIndexOutboxSender(OutboxMessageService outboxMessageService,
                                          ProductSearchIndexRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    /**
     * 写入商品搜索索引变更 Outbox 消息，并立即尝试投递一次 RabbitMQ。
     *
     * @param event 商品搜索索引变更事件
     */
    @Override
    public void send(ProductSearchIndexChangedEvent event) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                aggregateId(event),
                properties.getEventExchange(),
                properties.getEventRoutingKey(),
                event
        );
    }

    private String aggregateId(ProductSearchIndexChangedEvent event) {
        return event.productId() + ":" + event.eventType();
    }
}
