package com.tuzki.mall.test;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Redis 测试隔离监听器，在 Spring 集成测试类前后清理测试 Redis DB 中的 mall 业务 Key。
 */
public class RedisTestIsolationListener extends AbstractTestExecutionListener {

    private final RedisTestDataCleaner redisTestDataCleaner = new RedisTestDataCleaner();

    @Override
    public void beforeTestClass(TestContext testContext) {
        clean(testContext);
    }

    @Override
    public void afterTestClass(TestContext testContext) {
        clean(testContext);
    }

    private void clean(TestContext testContext) {
        RedissonClient redissonClient;
        try {
            redissonClient = testContext.getApplicationContext().getBean(RedissonClient.class);
        } catch (NoSuchBeanDefinitionException ignored) {
            return;
        }
        redisTestDataCleaner.clean(redissonClient);
    }
}
