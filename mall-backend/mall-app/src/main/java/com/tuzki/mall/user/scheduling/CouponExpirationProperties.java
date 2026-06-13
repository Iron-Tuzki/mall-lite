package com.tuzki.mall.user.scheduling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户优惠券过期任务配置属性，集中维护任务开关和固定延迟执行间隔。
 */
@Component
@ConfigurationProperties(prefix = "mall.coupon.expiration")
public class CouponExpirationProperties {

    private Boolean enabled = true;

    private Long fixedDelayMs = 60_000L;

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
}
