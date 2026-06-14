package com.tuzki.mall.admin;

import com.tuzki.mall.admin.product.service.ProductCacheInvalidationService;
import com.tuzki.mall.product.service.ProductDetailCacheService;
import com.tuzki.mall.product.service.ProductHotDetailCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCacheInvalidationServiceTest {

    @Test
    void invalidateProductDetailAfterCommitDeletesNowAndSchedulesDelayedDelete() {
        ProductDetailCacheService productDetailCacheService = mock(ProductDetailCacheService.class);
        ProductHotDetailCacheService productHotDetailCacheService = mock(ProductHotDetailCacheService.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            Runnable delayedTask = invocation.getArgument(0);
            delayedTask.run();
            return scheduledFuture;
        });

        ProductCacheInvalidationService service = new ProductCacheInvalidationService(
                productDetailCacheService,
                productHotDetailCacheService,
                taskScheduler,
                500);

        service.invalidateProductDetailAfterCommit(910036L);

        verify(productDetailCacheService, times(2)).invalidate(910036L);
        verify(productHotDetailCacheService, times(2)).invalidate(910036L);
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }
}
