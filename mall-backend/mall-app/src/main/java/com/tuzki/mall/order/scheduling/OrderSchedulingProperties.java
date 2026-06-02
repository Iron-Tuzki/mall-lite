package com.tuzki.mall.order.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单调度配置属性，集中维护超时订单补偿和失败队列告警的执行参数。
 */
@Component
@ConfigurationProperties(prefix = "mall.order.scheduling")
public class OrderSchedulingProperties {

    private Long compensationFixedDelayMs = 60_000L;

    private Long alertFixedDelayMs = 60_000L;

    private Integer timeoutMinutes = 30;

    private Integer batchSize = 100;

    public Long getCompensationFixedDelayMs() {
        return compensationFixedDelayMs;
    }

    public void setCompensationFixedDelayMs(Long compensationFixedDelayMs) {
        this.compensationFixedDelayMs = compensationFixedDelayMs;
    }

    public Long getAlertFixedDelayMs() {
        return alertFixedDelayMs;
    }

    public void setAlertFixedDelayMs(Long alertFixedDelayMs) {
        this.alertFixedDelayMs = alertFixedDelayMs;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
