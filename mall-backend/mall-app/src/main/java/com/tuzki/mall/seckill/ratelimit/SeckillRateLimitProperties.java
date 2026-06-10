package com.tuzki.mall.seckill.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀接口 Redis 防刷配置，集中维护用户维度和 IP 维度滑动窗口限流参数。
 */
@Component
@ConfigurationProperties(prefix = "mall.seckill.rate-limit")
public class SeckillRateLimitProperties {

    private Boolean enabled = true;

    private Integer windowSeconds = 5;

    private Integer userLimit = 3;

    private Integer ipLimit = 10;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Integer getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(Integer userLimit) {
        this.userLimit = userLimit;
    }

    public Integer getIpLimit() {
        return ipLimit;
    }

    public void setIpLimit(Integer ipLimit) {
        this.ipLimit = ipLimit;
    }
}
