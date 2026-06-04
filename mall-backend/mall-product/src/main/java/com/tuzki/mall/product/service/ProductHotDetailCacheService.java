package com.tuzki.mall.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.config.ProductCacheProperties;
import com.tuzki.mall.product.vo.ProductDetailVO;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 热门商品详情缓存服务，使用空值缓存、随机 TTL 和分布式锁互斥重建保护高热商品详情查询。
 */
@Service
public class ProductHotDetailCacheService {

    private static final String NULL_VALUE = "__NULL__";

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final ProductCacheProperties productCacheProperties;

    public ProductHotDetailCacheService(RedissonClient redissonClient,
                                        ObjectMapper objectMapper,
                                        ProductCacheProperties productCacheProperties) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.productCacheProperties = productCacheProperties;
    }

    /**
     * 查询热门商品详情缓存，缓存未命中时通过分布式锁互斥回源并重建缓存。
     *
     * @param productId 商品 ID
     * @param loader 回源查询函数，返回 null 表示商品不存在
     * @return 商品详情；不存在或回源失败时返回 null
     */
    public ProductDetailVO getOrLoad(Long productId, Supplier<ProductDetailVO> loader) {
        CacheLookup cacheLookup = readCache(productId);
        if (cacheLookup.hit()) {
            return cacheLookup.productDetailVO();
        }

        RLock lock = redissonClient.getLock(productCacheProperties.getHotDetailLockKeyPrefix() + productId);
        boolean locked = false;
        try {
            locked = lock.tryLock(
                    productCacheProperties.getHotDetailLockWaitSeconds(),
                    productCacheProperties.getHotDetailLockLeaseSeconds(),
                    TimeUnit.SECONDS);
            if (!locked) {
                // 未抢到锁则重试查询缓存
                return waitForRebuiltCache(productId);
            }
            //抢到锁后再次查询缓存，防止其他线程重建缓存
            CacheLookup doubleCheckedCacheLookup = readCache(productId);
            if (doubleCheckedCacheLookup.hit()) {
                return doubleCheckedCacheLookup.productDetailVO();
            }

            ProductDetailVO loadedProductDetailVO = loader.get();
            if (loadedProductDetailVO == null) {
                putNullValue(productId);
                return null;
            }
            // 重建缓存
            put(productId, loadedProductDetailVO);
            return loadedProductDetailVO;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(503, "hot product detail is rebuilding");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private ProductDetailVO waitForRebuiltCache(Long productId) throws InterruptedException {
        int retryTimes = Math.max(productCacheProperties.getHotDetailLockRetryTimes(), 0);
        long retryIntervalMs = Math.max(productCacheProperties.getHotDetailLockRetryIntervalMs(), 0);
        for (int retryIndex = 0; retryIndex < retryTimes; retryIndex++) {
            if (retryIntervalMs > 0) {
                TimeUnit.MILLISECONDS.sleep(retryIntervalMs);
            }
            CacheLookup cacheLookup = readCache(productId);
            if (cacheLookup.hit()) {
                return cacheLookup.productDetailVO();
            }
        }
        throw new BusinessException(503, "hot product detail is rebuilding");
    }

    private CacheLookup readCache(Long productId) {
        String cacheValue = getBucket(productId).get();
        if (cacheValue == null) {
            return new CacheLookup(false, null);
        }
        // 返回空值
        if (NULL_VALUE.equals(cacheValue)) {
            return new CacheLookup(true, null);
        }
        try {
            return new CacheLookup(true, objectMapper.readValue(cacheValue, ProductDetailVO.class));
        } catch (JsonProcessingException exception) {
            getBucket(productId).delete();
            return new CacheLookup(false, null);
        }
    }

    private void put(Long productId, ProductDetailVO productDetailVO) {
        try {
            String cacheValue = objectMapper.writeValueAsString(productDetailVO);
            getBucket(productId).set(cacheValue, buildHotDetailTtlSeconds(), TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize hot product detail cache failed", exception);
        }
    }

    private void putNullValue(Long productId) {
        long ttlSeconds = TimeUnit.MINUTES.toSeconds(productCacheProperties.getHotDetailNullTtlMinutes());
        getBucket(productId).set(NULL_VALUE, ttlSeconds, TimeUnit.SECONDS);
    }

    private RBucket<String> getBucket(Long productId) {
        return redissonClient.getBucket(
                productCacheProperties.getHotDetailKeyPrefix() + productId,
                StringCodec.INSTANCE);
    }

    private long buildHotDetailTtlSeconds() {
        long baseSeconds = TimeUnit.MINUTES.toSeconds(productCacheProperties.getHotDetailTtlMinutes());
        long randomMinutes = Math.max(productCacheProperties.getHotDetailRandomTtlMinutes(), 0);
        long randomSeconds = randomMinutes == 0
                ? 0
                : TimeUnit.MINUTES.toSeconds(ThreadLocalRandom.current().nextLong(randomMinutes + 1));
        return baseSeconds + randomSeconds;
    }

    private record CacheLookup(boolean hit, ProductDetailVO productDetailVO) {
    }
}
