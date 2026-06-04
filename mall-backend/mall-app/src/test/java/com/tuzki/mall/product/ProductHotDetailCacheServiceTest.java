package com.tuzki.mall.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.config.ProductCacheProperties;
import com.tuzki.mall.product.service.ProductHotDetailCacheService;
import com.tuzki.mall.product.vo.ProductDetailVO;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 热门商品详情缓存服务测试，验证空值缓存、随机 TTL 和分布式锁互斥重建缓存。
 */
class ProductHotDetailCacheServiceTest {

    @Test
    void cacheHitReturnsCachedProductWithoutLoadingDatabase() throws Exception {
        ProductDetailVO cachedProduct = productDetail(1001L, "Cached Hot Product");
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1001L);
        when(bucket.get()).thenReturn(new ObjectMapper().writeValueAsString(cachedProduct));
        Supplier<ProductDetailVO> loader = mockLoader();
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        ProductDetailVO result = cacheService.getOrLoad(1001L, loader);

        assertThat(result.getId()).isEqualTo(1001L);
        assertThat(result.getName()).isEqualTo("Cached Hot Product");
        verify(loader, never()).get();
    }

    @Test
    void missingProductWritesNullCacheWhenLockAcquired() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1002L);
        RLock lock = mockLock(redissonClient, 1002L, true);
        when(bucket.get()).thenReturn(null);
        Supplier<ProductDetailVO> loader = mockLoader();
        when(loader.get()).thenReturn(null);
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        ProductDetailVO result = cacheService.getOrLoad(1002L, loader);

        assertThat(result).isNull();
        verify(bucket).set(eq("__NULL__"), eq(300L), eq(TimeUnit.SECONDS));
        verify(lock).unlock();
    }

    @Test
    void lockAcquiredDoubleChecksCacheBeforeLoadingDatabase() throws Exception {
        ProductDetailVO cachedProduct = productDetail(1003L, "Rebuilt By Other Request");
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1003L);
        RLock lock = mockLock(redissonClient, 1003L, true);
        when(bucket.get())
                .thenReturn(null)
                .thenReturn(new ObjectMapper().writeValueAsString(cachedProduct));
        Supplier<ProductDetailVO> loader = mockLoader();
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        ProductDetailVO result = cacheService.getOrLoad(1003L, loader);

        assertThat(result.getName()).isEqualTo("Rebuilt By Other Request");
        verify(loader, never()).get();
        verify(lock).unlock();
    }

    @Test
    void lockAcquiredLoadsDatabaseAndWritesRandomTtlCache() throws Exception {
        ProductDetailVO loadedProduct = productDetail(1004L, "Loaded Hot Product");
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1004L);
        RLock lock = mockLock(redissonClient, 1004L, true);
        when(bucket.get()).thenReturn(null);
        Supplier<ProductDetailVO> loader = mockLoader();
        when(loader.get()).thenReturn(loadedProduct);
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        ProductDetailVO result = cacheService.getOrLoad(1004L, loader);

        assertThat(result.getName()).isEqualTo("Loaded Hot Product");
        verify(bucket).set(any(String.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(lock).unlock();
    }

    @Test
    void lockNotAcquiredWaitsForRebuiltCacheWithoutLoadingDatabase() throws Exception {
        ProductDetailVO rebuiltProduct = productDetail(1005L, "Rebuilt Hot Product");
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1005L);
        RLock lock = mockLock(redissonClient, 1005L, false);
        when(bucket.get())
                .thenReturn(null)
                .thenReturn(null)
                .thenReturn(new ObjectMapper().writeValueAsString(rebuiltProduct));
        Supplier<ProductDetailVO> loader = mockLoader();
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        ProductDetailVO result = cacheService.getOrLoad(1005L, loader);

        assertThat(result.getName()).isEqualTo("Rebuilt Hot Product");
        verify(loader, never()).get();
        verify(bucket, never()).set(any(String.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(lock, never()).unlock();
    }

    @Test
    void lockNotAcquiredThrowsRebuildingExceptionWithoutLoadingDatabaseWhenCacheStillMissing() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> bucket = mockBucket(redissonClient, 1006L);
        RLock lock = mockLock(redissonClient, 1006L, false);
        when(bucket.get()).thenReturn(null);
        Supplier<ProductDetailVO> loader = mockLoader();
        ProductHotDetailCacheService cacheService = cacheService(redissonClient);

        assertThatThrownBy(() -> cacheService.getOrLoad(1006L, loader))
                .isInstanceOf(BusinessException.class)
                .hasMessage("hot product detail is rebuilding");

        verify(loader, never()).get();
        verify(bucket, never()).set(any(String.class), anyLong(), eq(TimeUnit.SECONDS));
        verify(lock, never()).unlock();
    }

    @SuppressWarnings("unchecked")
    private RBucket<String> mockBucket(RedissonClient redissonClient, Long productId) {
        RBucket<String> bucket = mock(RBucket.class);
        when(redissonClient.<String>getBucket("mall:product:hot:detail:" + productId, StringCodec.INSTANCE))
                .thenReturn(bucket);
        return bucket;
    }

    private RLock mockLock(RedissonClient redissonClient, Long productId, boolean acquired) throws Exception {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("mall:product:hot:detail:lock:" + productId)).thenReturn(lock);
        when(lock.tryLock(1L, 10L, TimeUnit.SECONDS)).thenReturn(acquired);
        return lock;
    }

    @SuppressWarnings("unchecked")
    private Supplier<ProductDetailVO> mockLoader() {
        return mock(Supplier.class);
    }

    private ProductHotDetailCacheService cacheService(RedissonClient redissonClient) {
        ProductCacheProperties properties = new ProductCacheProperties();
        properties.setHotDetailLockRetryTimes(2);
        properties.setHotDetailLockRetryIntervalMs(0);
        return new ProductHotDetailCacheService(redissonClient, new ObjectMapper(), properties);
    }

    private ProductDetailVO productDetail(Long productId, String name) {
        ProductDetailVO productDetailVO = new ProductDetailVO();
        productDetailVO.setId(productId);
        productDetailVO.setName(name);
        return productDetailVO;
    }
}
