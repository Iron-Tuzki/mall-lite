package com.tuzki.mall;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 测试隔离配置测试，验证集成测试使用独立 Redis DB，避免污染本地开发数据。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class RedisTestIsolationConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    void springBootTestsUseDedicatedRedisDatabase() {
        assertThat(environment.getProperty("spring.data.redis.database")).isEqualTo("1");
    }
}
