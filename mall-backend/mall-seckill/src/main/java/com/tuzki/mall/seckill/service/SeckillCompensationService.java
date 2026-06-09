package com.tuzki.mall.seckill.service;

import com.tuzki.mall.seckill.entity.SeckillRequest;
import com.tuzki.mall.seckill.mapper.SeckillRequestMapper;
import com.tuzki.mall.seckill.redis.SeckillRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀补偿服务，扫描长时间停留在 Redis 预扣成功状态的请求流水并回补 Redis 占用。
 */
@Service
public class SeckillCompensationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillCompensationService.class);

    private static final String COMPENSATION_SUCCESS_REASON = "seckill request compensation succeeded";

    private final SeckillRequestMapper seckillRequestMapper;

    private final SeckillRedisService seckillRedisService;

    public SeckillCompensationService(SeckillRequestMapper seckillRequestMapper,
                                      SeckillRedisService seckillRedisService) {
        this.seckillRequestMapper = seckillRequestMapper;
        this.seckillRedisService = seckillRedisService;
    }

    /**
     * 批量补偿超时未完成的 Redis 预扣成功请求流水。
     *
     * @param timeoutBefore 超时边界时间，更新时间早于等于该时间才会被补偿
     * @param batchSize 单批补偿数量
     * @param maxRetryCount 最大补偿重试次数
     * @return 本轮成功标记为已补偿的流水数量
     */
    public int compensateTimedOutPreDeductedRequests(LocalDateTime timeoutBefore,
                                                     Integer batchSize,
                                                     Integer maxRetryCount) {
        List<SeckillRequest> requests = seckillRequestMapper.listTimedOutPreDeducted(
                timeoutBefore,
                normalizePositive(batchSize, 100),
                normalizePositive(maxRetryCount, 3));
        int compensatedCount = 0;
        for (SeckillRequest request : requests) {
            if (compensateSingleRequest(request)) {
                compensatedCount++;
            }
        }
        return compensatedCount;
    }

    private boolean compensateSingleRequest(SeckillRequest request) {
        try {
            seckillRedisService.compensate(
                    request.getSeckillSkuId(),
                    request.getUserId(),
                    request.getRequestId(),
                    request.getQuantity());
            return seckillRequestMapper.markCompensatedIfPreDeducted(
                    request.getId(),
                    COMPENSATION_SUCCESS_REASON) == 1;
        } catch (RuntimeException exception) {
            LOGGER.error("seckill request compensation failed, requestId={}, seckillSkuId={}, userId={}",
                    request.getRequestId(), request.getSeckillSkuId(), request.getUserId(), exception);
            // 补偿失败记录重试次数
            seckillRequestMapper.increaseCompensationRetry(request.getId(), normalizeReason(exception.getMessage()));
            return false;
        }
    }

    private Integer normalizePositive(Integer value, Integer defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private String normalizeReason(String failReason) {
        if (failReason == null || failReason.isBlank()) {
            return "seckill request compensation failed";
        }
        return failReason.length() <= 255 ? failReason : failReason.substring(0, 255);
    }
}
