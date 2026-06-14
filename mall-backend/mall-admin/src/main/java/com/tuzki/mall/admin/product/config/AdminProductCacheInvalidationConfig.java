package com.tuzki.mall.admin.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 后台商品缓存失效配置，提供异步延迟二删使用的独立调度线程池。
 */
@Configuration
public class AdminProductCacheInvalidationConfig {

    @Bean
    public ThreadPoolTaskScheduler adminProductCacheInvalidationTaskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2);
        taskScheduler.setThreadNamePrefix("admin-product-cache-invalidate-");
        taskScheduler.setRemoveOnCancelPolicy(true);
        return taskScheduler;
    }
}
