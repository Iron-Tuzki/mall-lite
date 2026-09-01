package com.tuzki.mall.admin.product.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 商品搜索索引同步协调服务，负责把商品写库事务中的索引变更转换为 Outbox 异步事件。
 */
@Service
public class ProductSearchIndexSynchronizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSearchIndexSynchronizationService.class);

    private final ProductSearchIndexEventSender productSearchIndexEventSender;

    public ProductSearchIndexSynchronizationService(ProductSearchIndexEventSender productSearchIndexEventSender) {
        this.productSearchIndexEventSender = productSearchIndexEventSender;
    }

    public void syncProductAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        sendEvent(productId, ProductSearchIndexChangedEventType.SYNC);
    }

    public void deleteProductAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        sendEvent(productId, ProductSearchIndexChangedEventType.DELETE);
    }

    private void sendEvent(Long productId, ProductSearchIndexChangedEventType eventType) {
        try {
            productSearchIndexEventSender.send(new ProductSearchIndexChangedEvent(productId, eventType));
        } catch (RuntimeException exception) {
            LOGGER.warn("send product search index event failed, productId={}, eventType={}", productId, eventType, exception);
        }
    }
}
