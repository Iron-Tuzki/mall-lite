package com.tuzki.mall.seckill.scheduling;

import com.tuzki.mall.scheduling.lock.RedisDistributedLock;
import com.tuzki.mall.seckill.service.SeckillCompensationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 秒杀后台补偿任务，定期扫描超时停留在 Redis 预扣成功状态的秒杀请求流水。
 */
@Component
public class SeckillCompensationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillCompensationTask.class);

    private static final String LOCK_KEY = "mall:seckill:compensation";

    private final SeckillCompensationService seckillCompensationService;

    private final SeckillCompensationProperties properties;

    public SeckillCompensationTask(SeckillCompensationService seckillCompensationService,
                                   SeckillCompensationProperties properties) {
        this.seckillCompensationService = seckillCompensationService;
        this.properties = properties;
    }

    /**
     * 扫描并补偿超时未完成的 Redis 预扣流水。
     */
    @Scheduled(
            initialDelayString = "${mall.seckill.compensation.fixed-delay-ms:60000}",
            fixedDelayString = "${mall.seckill.compensation.fixed-delay-ms:60000}"
    )
    @RedisDistributedLock(LOCK_KEY)
    public void compensateTimedOutRequests() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        long begin = System.currentTimeMillis();
        LocalDateTime timeoutBefore = LocalDateTime.now().minusSeconds(normalizePositive(properties.getTimeoutSeconds(), 120));
        int compensatedCount = seckillCompensationService.compensateTimedOutPreDeductedRequests(
                timeoutBefore,
                normalizePositive(properties.getBatchSize(), 100),
                normalizePositive(properties.getMaxRetryCount(), 3));
        long end = System.currentTimeMillis();
        LOGGER.info("seckill compensation task finished, compensated={}, cost={}ms",
                compensatedCount, (end - begin));
    }

    private Integer normalizePositive(Integer value, Integer defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }
}
