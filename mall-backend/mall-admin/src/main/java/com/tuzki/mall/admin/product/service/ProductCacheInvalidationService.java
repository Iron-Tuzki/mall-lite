package com.tuzki.mall.admin.product.service;

import com.tuzki.mall.product.service.ProductDetailCacheService;
import com.tuzki.mall.product.service.ProductHotDetailCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * 商品详情缓存失效服务，在商品写入事务提交后执行立即删除，并异步调度一次延迟删除。
 */
@Service
public class ProductCacheInvalidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCacheInvalidationService.class);

    private final ProductDetailCacheService productDetailCacheService;

    private final ProductHotDetailCacheService productHotDetailCacheService;

    private final TaskScheduler adminProductCacheInvalidationTaskScheduler;

    private final long delayedDeleteMillis;

    public ProductCacheInvalidationService(ProductDetailCacheService productDetailCacheService,
                                           ProductHotDetailCacheService productHotDetailCacheService,
                                           @Qualifier("adminProductCacheInvalidationTaskScheduler")
                                           TaskScheduler adminProductCacheInvalidationTaskScheduler,
                                           @Value("${mall.product.cache.delayed-delete-millis:500}")
                                           long delayedDeleteMillis) {
        this.productDetailCacheService = productDetailCacheService;
        this.productHotDetailCacheService = productHotDetailCacheService;
        this.adminProductCacheInvalidationTaskScheduler = adminProductCacheInvalidationTaskScheduler;
        this.delayedDeleteMillis = delayedDeleteMillis;
    }

    public void invalidateProductDetailAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        runAfterCommit(() -> invalidateTwice(productId));
    }

    private void invalidateTwice(Long productId) {
        invalidateOnce(productId);
        scheduleDelayedInvalidate(productId);
    }

    private void scheduleDelayedInvalidate(Long productId) {
        long safeDelayMillis = Math.max(delayedDeleteMillis, 0);
        try {
            adminProductCacheInvalidationTaskScheduler.schedule(
                    () -> invalidateOnce(productId),
                    Instant.now().plusMillis(safeDelayMillis));
        } catch (RuntimeException exception) {
            LOGGER.warn("schedule delayed product detail cache invalidation failed, productId={}", productId, exception);
        }
    }

    private void invalidateOnce(Long productId) {
        try {
            productDetailCacheService.invalidate(productId);
            productHotDetailCacheService.invalidate(productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("invalidate product detail cache failed, productId={}", productId, exception);
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
