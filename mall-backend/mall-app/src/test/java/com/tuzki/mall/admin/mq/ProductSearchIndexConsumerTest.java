package com.tuzki.mall.admin.mq;

import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEvent;
import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEventType;
import com.tuzki.mall.admin.product.search.ProductSearchIndexService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品搜索索引消费者测试，验证不同事件类型会触发对应的 Elasticsearch 同步动作。
 */
class ProductSearchIndexConsumerTest {

    @Test
    void syncEventRefreshesProductSearchDocument() {
        ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
        ProductSearchIndexChangedEvent event = new ProductSearchIndexChangedEvent(
                100L,
                ProductSearchIndexChangedEventType.SYNC
        );

        new ProductSearchIndexConsumer(productSearchIndexService).handle(event);

        verify(productSearchIndexService).syncProduct(100L);
    }

    @Test
    void deleteEventRemovesProductSearchDocument() {
        ProductSearchIndexService productSearchIndexService = mock(ProductSearchIndexService.class);
        ProductSearchIndexChangedEvent event = new ProductSearchIndexChangedEvent(
                100L,
                ProductSearchIndexChangedEventType.DELETE
        );

        new ProductSearchIndexConsumer(productSearchIndexService).handle(event);

        verify(productSearchIndexService).deleteProduct(100L);
    }
}
