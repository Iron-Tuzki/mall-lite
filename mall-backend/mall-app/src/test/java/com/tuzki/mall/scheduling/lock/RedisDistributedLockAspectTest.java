package com.tuzki.mall.scheduling.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Redis 分布式锁切面测试，验证任务抢锁成功和失败时的执行行为。
 */
class RedisDistributedLockAspectTest {

    @Test
    void proceedsAndUnlocksWhenLockIsAcquired() throws Throwable {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RedisDistributedLock annotation = mock(RedisDistributedLock.class);
        when(annotation.value()).thenReturn("mall:order:timeout-compensation");
        when(redissonClient.getLock("mall:order:timeout-compensation")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        new RedisDistributedLockAspect(redissonClient).around(joinPoint, annotation);

        verify(joinPoint).proceed();
        verify(lock).unlock();
    }

    @Test
    void skipsExecutionWhenLockIsHeldByAnotherInstance() throws Throwable {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RedisDistributedLock annotation = mock(RedisDistributedLock.class);
        when(annotation.value()).thenReturn("mall:order:timeout-compensation");
        when(redissonClient.getLock("mall:order:timeout-compensation")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);

        new RedisDistributedLockAspect(redissonClient).around(joinPoint, annotation);

        verify(joinPoint, never()).proceed();
        verify(lock, never()).unlock();
    }
}
