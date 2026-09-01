package com.tuzki.mall.admin.mq;

import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEvent;
import com.tuzki.mall.admin.product.search.ProductSearchIndexChangedEventType;
import com.tuzki.mall.admin.product.search.ProductSearchIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品搜索索引事件消费者，负责异步消费商品变更事件并同步 Elasticsearch 商品搜索文档。
 */
@Component
public class ProductSearchIndexConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSearchIndexConsumer.class);

    private final ProductSearchIndexService productSearchIndexService;

    public ProductSearchIndexConsumer(ProductSearchIndexService productSearchIndexService) {
        this.productSearchIndexService = productSearchIndexService;
    }

    /**
     * 消费商品搜索索引变更事件。
     *
     * @param event 商品搜索索引变更事件
     */
    @RabbitListener(queues = "${mall.product.search-index-rabbit.event-queue}")
    public void handle(ProductSearchIndexChangedEvent event) {
        LOGGER.info("商品搜索索引变更事件类型：{}，商品 ID：{}", event.eventType(), event.productId());
        if (event.eventType() == ProductSearchIndexChangedEventType.DELETE) {
            productSearchIndexService.deleteProduct(event.productId());
            return;
        }
        productSearchIndexService.syncProduct(event.productId());
    }
}
