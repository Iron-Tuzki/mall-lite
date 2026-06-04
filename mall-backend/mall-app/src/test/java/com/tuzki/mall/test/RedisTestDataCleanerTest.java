package com.tuzki.mall.test;

import org.junit.jupiter.api.Test;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 测试数据清理器测试，验证只清理 mall 业务前缀 Key。
 */
class RedisTestDataCleanerTest {

    @Test
    void cleanDeletesOnlyMallBusinessKeys() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RKeys keys = mock(RKeys.class);
        when(redissonClient.getKeys()).thenReturn(keys);

        new RedisTestDataCleaner().clean(redissonClient);

        verify(keys).deleteByPattern("mall:*");
    }
}
