package com.tuzki.mall.seckill.scheduling;

import com.tuzki.mall.seckill.service.SeckillCompensationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 秒杀补偿任务测试，验证任务按配置扫描超时预扣流水并支持开关控制。
 */
class SeckillCompensationTaskTest {

    @Test
    void compensatesTimedOutPreDeductedRequestsWithConfiguredProperties() {
        SeckillCompensationService compensationService = mock(SeckillCompensationService.class);
        SeckillCompensationProperties properties = new SeckillCompensationProperties();
        properties.setTimeoutSeconds(120);
        properties.setBatchSize(50);
        properties.setMaxRetryCount(5);

        new SeckillCompensationTask(compensationService, properties).compensateTimedOutRequests();

        verify(compensationService).compensateTimedOutPreDeductedRequests(
                any(LocalDateTime.class),
                eq(50),
                eq(5));
    }

    @Test
    void skipsCompensationWhenDisabled() {
        SeckillCompensationService compensationService = mock(SeckillCompensationService.class);
        SeckillCompensationProperties properties = new SeckillCompensationProperties();
        properties.setEnabled(false);

        new SeckillCompensationTask(compensationService, properties).compensateTimedOutRequests();

        verify(compensationService, never()).compensateTimedOutPreDeductedRequests(
                any(LocalDateTime.class),
                any(Integer.class),
                any(Integer.class));
    }
}
