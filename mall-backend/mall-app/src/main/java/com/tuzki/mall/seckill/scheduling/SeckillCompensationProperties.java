package com.tuzki.mall.seckill.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀补偿调度配置属性，集中维护补偿任务开关、执行间隔、超时阈值和重试参数。
 */
@Component
@ConfigurationProperties(prefix = "mall.seckill.compensation")
public class SeckillCompensationProperties {

    private Boolean enabled = true;

    private Long fixedDelayMs = 60_000L;

    private Integer timeoutSeconds = 120;

    private Integer batchSize = 100;

    private Integer maxRetryCount = 3;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public void setFixedDelayMs(Long fixedDelayMs) {
        this.fixedDelayMs = fixedDelayMs;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
}
