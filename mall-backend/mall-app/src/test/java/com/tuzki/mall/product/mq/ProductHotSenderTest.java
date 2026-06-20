package com.tuzki.mall.product.mq;

import com.tuzki.mall.config.rabbit.ProductHotRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品热点事件发送器测试，验证热点事件会通过 Outbox 可靠投递。
 */
class ProductHotSenderTest {

    @Test
    void sendCreatesOutboxMessageUsingHotEventExchangeAndRoutingKey() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        ProductHotRabbitProperties properties = new ProductHotRabbitProperties();
        ProductHotEvent event = new ProductHotEvent(
                "event-1", 100L, ProductHotAction.VIEW, LocalDateTime.now());

        new ProductHotSender(outboxMessageService, properties).send(event);

        verify(outboxMessageService).createAndPublish(
                "PRODUCT_HOT",
                "event-1",
                properties.getEventExchange(),
                properties.getEventRoutingKey(),
                event
        );
    }
}
