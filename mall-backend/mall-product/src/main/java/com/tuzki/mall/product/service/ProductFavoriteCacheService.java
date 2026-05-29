package com.tuzki.mall.product.service;

import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品收藏缓存服务，使用 Redis Set 缓存用户收藏商品 ID，并使用 loaded 标记区分空集合和未加载状态。
 */
@Service
public class ProductFavoriteCacheService {

    private static final String FAVORITE_KEY_PREFIX = "mall:user:favorites:";

    private static final String FAVORITE_LOADED_KEY_PREFIX = "mall:user:favorites:loaded:";

    private static final Duration BASE_TTL = Duration.ofHours(24);

    private static final int RANDOM_TTL_SECONDS = 30 * 60;

    private final RedissonClient redissonClient;

    public ProductFavoriteCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 判断指定用户收藏缓存是否已经完整加载过。
     *
     * @param userId 用户 ID
     * @return loaded 标记存在时返回 true，否则返回 false
     */
    public boolean isLoaded(Long userId) {
        return loadedBucket(userId).isExists();
    }

    /**
     * 使用 MySQL 查询出的完整收藏商品 ID 重建 Redis 缓存。
     *
     * @param userId 用户 ID
     * @param productIds 完整收藏商品 ID 集合
     */
    public void rebuild(Long userId, Collection<Long> productIds) {
        RSet<String> favoriteSet = favoriteSet(userId);
        favoriteSet.delete();
        if (productIds != null && !productIds.isEmpty()) {
            List<String> cacheValues = productIds.stream()
                    .map(String::valueOf)
                    .toList();
            favoriteSet.addAll(cacheValues);
            favoriteSet.expire(randomTtl());
        }
        markLoaded(userId);
    }

    /**
     * 从 Redis 判断指定商品是否已收藏。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @return 已收藏返回 true，否则返回 false
     */
    public boolean contains(Long userId, Long productId) {
        return favoriteSet(userId).contains(String.valueOf(productId));
    }

    /**
     * 从 Redis 批量判断指定商品列表是否已收藏。
     *
     * @param userId 用户 ID
     * @param productIds 商品 ID 列表
     * @return 商品 ID 与收藏状态映射
     */
    public Map<Long, Boolean> batchContains(Long userId, List<Long> productIds) {
        RSet<String> favoriteSet = favoriteSet(userId);
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long productId : productIds) {
            result.put(productId, favoriteSet.contains(String.valueOf(productId)));
        }
        return result;
    }

    /**
     * 当缓存已完整加载时，向收藏缓存增量添加商品 ID。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     */
    public void addIfLoaded(Long userId, Long productId) {
        if (!isLoaded(userId)) {
            return;
        }
        RSet<String> favoriteSet = favoriteSet(userId);
        favoriteSet.add(String.valueOf(productId));
        favoriteSet.expire(randomTtl());
        markLoaded(userId);
    }

    /**
     * 当缓存已完整加载时，从收藏缓存增量移除商品 ID。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     */
    public void removeIfLoaded(Long userId, Long productId) {
        if (!isLoaded(userId)) {
            return;
        }
        RSet<String> favoriteSet = favoriteSet(userId);
        favoriteSet.remove(String.valueOf(productId));
        if (favoriteSet.isExists()) {
            favoriteSet.expire(randomTtl());
        }
        markLoaded(userId);
    }

    /**
     * 删除 loaded 标记，使下一次查询回源 MySQL 重建缓存。
     *
     * @param userId 用户 ID
     */
    public void invalidateLoaded(Long userId) {
        loadedBucket(userId).delete();
    }

    private void markLoaded(Long userId) {
        loadedBucket(userId).set("1", randomTtl());
    }

    private RSet<String> favoriteSet(Long userId) {
        return redissonClient.getSet(FAVORITE_KEY_PREFIX + userId, StringCodec.INSTANCE);
    }

    private RBucket<String> loadedBucket(Long userId) {
        return redissonClient.getBucket(FAVORITE_LOADED_KEY_PREFIX + userId, StringCodec.INSTANCE);
    }

    private Duration randomTtl() {
        return BASE_TTL.plusSeconds(ThreadLocalRandom.current().nextInt(RANDOM_TTL_SECONDS + 1));
    }
}
