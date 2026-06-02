package com.tuzki.mall.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.product.config.ProductCacheProperties;
import com.tuzki.mall.product.vo.ProductDetailVO;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商品详情缓存服务，负责从 Redis 读取和写入商品详情 JSON 数据。
 */
@Service
public class ProductDetailCacheService {

    private static final String NULL_VALUE = "__NULL__";

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final ProductCacheProperties productCacheProperties;

    public ProductDetailCacheService(RedissonClient redissonClient, ObjectMapper objectMapper,
                                     ProductCacheProperties productCacheProperties) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.productCacheProperties = productCacheProperties;
    }

    public ProductDetailVO get(Long productId) {
        String cacheValue = getBucket(productId).get();
        if (cacheValue == null) {
            return null;
        }
        if (NULL_VALUE.equals(cacheValue)) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheValue, ProductDetailVO.class);
        } catch (JsonProcessingException exception) {
            getBucket(productId).delete();
            return null;
        }
    }

    public void put(Long productId, ProductDetailVO productDetailVO) {
        try {
            String cacheValue = objectMapper.writeValueAsString(productDetailVO);
            getBucket(productId).set(cacheValue, buildTtlSeconds(), TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize product detail cache failed", exception);
        }
    }

    public boolean isNullValueCached(Long productId) {
        return NULL_VALUE.equals(getBucket(productId).get());
    }

    public void putNullValue(Long productId) {
        long ttlSeconds = TimeUnit.MINUTES.toSeconds(productCacheProperties.getDetailNullTtlMinutes());
        getBucket(productId).set(NULL_VALUE, ttlSeconds, TimeUnit.SECONDS);
    }

    private RBucket<String> getBucket(Long productId) {
        String key = productCacheProperties.getDetailKeyPrefix() + productId;
        return redissonClient.getBucket(key, StringCodec.INSTANCE);
    }

    private long buildTtlSeconds() {
        // 基础过期时间增加随机偏移
        long baseSeconds = TimeUnit.MINUTES.toSeconds(productCacheProperties.getDetailTtlMinutes());
        long randomMinutes = Math.max(productCacheProperties.getDetailRandomTtlMinutes(), 0);
        long randomSeconds = randomMinutes == 0
                ? 0
                : TimeUnit.MINUTES.toSeconds(ThreadLocalRandom.current().nextLong(randomMinutes + 1));
        return baseSeconds + randomSeconds;
    }
}
