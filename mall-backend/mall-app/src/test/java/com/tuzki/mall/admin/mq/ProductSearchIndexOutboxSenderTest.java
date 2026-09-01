package com.tuzki.mall.admin.mq;

import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEvent;
import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEventType;
import com.tuzki.mall.config.rabbit.ProductSearchIndexRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品搜索索引 Outbox 发送器测试，验证商品变更事件会先写入 Outbox 再投递 RabbitMQ。
 */
class ProductSearchIndexOutboxSenderTest {

    @Test
    void sendCreatesProductSearchIndexOutboxMessageUsingConfiguredRabbitDestination() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        ProductSearchIndexRabbitProperties properties = new ProductSearchIndexRabbitProperties();
        properties.setEventExchange("mall.product.search-index.event.exchange");
        properties.setEventRoutingKey("mall.product.search-index.event.routing-key");
        ProductSearchIndexChangedEvent event = new ProductSearchIndexChangedEvent(
                100L,
                ProductSearchIndexChangedEventType.SYNC
        );

        new ProductSearchIndexOutboxSender(outboxMessageService, properties).send(event);

        verify(outboxMessageService).createAndPublish(
                "PRODUCT_SEARCH_INDEX",
                "100:SYNC",
                "mall.product.search-index.event.exchange",
                "mall.product.search-index.event.routing-key",
                event
        );
    }
}
