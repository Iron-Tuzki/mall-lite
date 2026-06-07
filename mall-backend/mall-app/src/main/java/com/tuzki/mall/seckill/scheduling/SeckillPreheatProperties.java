package com.tuzki.mall.seckill.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀预热调度配置属性，集中维护预热任务开关、执行间隔和提前预热时间窗口。
 */
@Component
@ConfigurationProperties(prefix = "mall.seckill.preheat")
public class SeckillPreheatProperties {

    private Boolean enabled = true;

    private Long fixedDelayMs = 60_000L;

    private Integer windowMinutes = 10;

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

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(Integer windowMinutes) {
        this.windowMinutes = windowMinutes;
    }
}
