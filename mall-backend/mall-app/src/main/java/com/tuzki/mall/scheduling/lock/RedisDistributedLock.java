package com.tuzki.mall.scheduling.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis 分布式锁注解，用于限制同一时刻只有一个应用实例执行目标方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisDistributedLock {

    /**
     * Redis 分布式锁键。
     *
     * @return Redis 分布式锁键
     */
    String value();
}
