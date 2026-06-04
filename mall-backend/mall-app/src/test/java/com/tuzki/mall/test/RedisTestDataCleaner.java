package com.tuzki.mall.test;

import org.redisson.api.RedissonClient;

/**
 * Redis 测试数据清理器，仅删除测试 Redis DB 中的 mall 业务前缀 Key。
 */
public class RedisTestDataCleaner {

    private static final String MALL_KEY_PATTERN = "mall:*";

    public void clean(RedissonClient redissonClient) {
        redissonClient.getKeys().deleteByPattern(MALL_KEY_PATTERN);
    }
}
