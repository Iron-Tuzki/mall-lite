package com.tuzki.mall.seckill.scheduling;

import com.tuzki.mall.scheduling.lock.RedisDistributedLock;
import com.tuzki.mall.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 秒杀活动库存预热任务，定期将当前和即将开始的秒杀活动库存写入 Redis。
 */
@Component
public class SeckillPreheatTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillPreheatTask.class);

    private static final String LOCK_KEY = "mall:seckill:preheat";

    private final SeckillService seckillService;

    private final SeckillPreheatProperties properties;

    public SeckillPreheatTask(SeckillService seckillService, SeckillPreheatProperties properties) {
        this.seckillService = seckillService;
        this.properties = properties;
    }

    /**
     * 预热当前进行中以及预热窗口内即将开始的秒杀活动库存。
     */
    @Scheduled(
            initialDelayString = "${mall.seckill.preheat.fixed-delay-ms:60000}",
            fixedDelayString = "${mall.seckill.preheat.fixed-delay-ms:60000}"
    )
    @RedisDistributedLock(LOCK_KEY)
    public void preheatUpcomingActivities() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        long begin = System.currentTimeMillis();
        Integer windowMinutes = properties.getWindowMinutes();
        Duration preheatWindow = Duration.ofMinutes(windowMinutes == null ? 0 : Math.max(0, windowMinutes));
        List<Long> activityIds = seckillService.listPreheatableActivityIds(preheatWindow);
        int successCount = 0;
        for (Long activityId : activityIds) {
            try {
                seckillService.preheatActivity(activityId);
                successCount++;
            } catch (RuntimeException exception) {
                LOGGER.error("seckill activity preheat failed, activityId={}", activityId, exception);
            }
        }
        long end = System.currentTimeMillis();
        LOGGER.info("seckill preheat task finished, matched={}, success={}, cost={}ms",
                activityIds.size(), successCount, (end - begin));
    }
}
