package com.tuzki.mall.scheduling.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Redis 分布式锁切面，使用 Redisson 非阻塞抢锁并在方法结束后释放当前线程持有的锁。
 */
@Aspect
@Component
public class RedisDistributedLockAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedLockAspect.class);

    private final RedissonClient redissonClient;

    public RedisDistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 在目标方法执行前抢占 Redis 分布式锁。
     *
     * @param joinPoint 目标方法连接点
     * @param redisDistributedLock Redis 分布式锁注解
     * @return 目标方法返回值；未抢到锁时返回 null
     * @throws Throwable 目标方法执行异常
     */
    @Around("@annotation(redisDistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint,
                         RedisDistributedLock redisDistributedLock) throws Throwable {
        RLock lock = redissonClient.getLock(redisDistributedLock.value());
        boolean acquired = lock.tryLock();
        if (!acquired) {
            LOGGER.debug("skip scheduled task because distributed lock is held, lockKey={}",
                    redisDistributedLock.value());
            return null;
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
