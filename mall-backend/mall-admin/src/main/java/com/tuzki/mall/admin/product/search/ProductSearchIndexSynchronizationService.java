package com.tuzki.mall.admin.product.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 商品搜索索引同步协调服务，负责在商品写入事务提交后触发 Elasticsearch 同步。
 */
@Service
public class ProductSearchIndexSynchronizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSearchIndexSynchronizationService.class);

    private final ProductSearchIndexService productSearchIndexService;

    public ProductSearchIndexSynchronizationService(ProductSearchIndexService productSearchIndexService) {
        this.productSearchIndexService = productSearchIndexService;
    }

    public void syncProductAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        runAfterCommit(() -> syncProduct(productId));
    }

    public void deleteProductAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        runAfterCommit(() -> deleteProduct(productId));
    }

    private void syncProduct(Long productId) {
        try {
            productSearchIndexService.syncProduct(productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("sync product search index failed, productId={}", productId, exception);
        }
    }

    private void deleteProduct(Long productId) {
        try {
            productSearchIndexService.deleteProduct(productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("delete product search index failed, productId={}", productId, exception);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
