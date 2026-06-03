package com.tuzki.mall.product.mq;

import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.service.ProductHotService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品热点事件消费者测试，验证消费端会将消息交给热点服务处理。
 */
class ProductHotConsumerTest {

    @Test
    void consumedMessageIsHandledByProductHotService() {
        ProductHotService productHotService = mock(ProductHotService.class);
        ProductHotEvent event = new ProductHotEvent("event-1", 100L, ProductHotAction.VIEW, LocalDateTime.now());

        new ProductHotConsumer(productHotService).handle(event);

        verify(productHotService).handleEvent(event);
    }
}
