package com.tuzki.mall.outbox.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Outbox 投递任务配置属性，集中维护补发任务的开关、频率和批量大小。
 */
@Component
@ConfigurationProperties(prefix = "mall.outbox.relay")
public class OutboxRelayProperties {

    private Boolean enabled = true;

    private Long fixedDelayMs = 60_000L;

    private Integer batchSize = 100;

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

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
