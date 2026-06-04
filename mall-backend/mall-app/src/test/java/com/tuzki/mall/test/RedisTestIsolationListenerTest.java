package com.tuzki.mall.test;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis 测试隔离监听器测试，验证没有 Redis 客户端的窄上下文测试不会被清理逻辑影响。
 */
class RedisTestIsolationListenerTest {

    @Test
    void beforeTestClassSkipsWhenRedissonClientIsAbsent() {
        TestContext testContext = mock(TestContext.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(testContext.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.getBean(RedissonClient.class))
                .thenThrow(new NoSuchBeanDefinitionException(RedissonClient.class));

        RedisTestIsolationListener listener = new RedisTestIsolationListener();

        assertDoesNotThrow(() -> listener.beforeTestClass(testContext));
    }
}
