package com.tuzki.mall.config.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 优惠券 RabbitMQ 配置属性，集中维护相关的交换机、队列、路由键和超时时间。
 */
@ConfigurationProperties(prefix = "mall.coupon.rabbit")
public class CouponRewardRabbitProperties {

    private String couponRewardExchange = "mall.coupon-reward.exchange";

    private String couponRewardQueue = "mall.coupon.queue";

    private String couponRewardRoutingKey = "mall.coupon.routing-key";

    private String failedExchange = "mall.coupon.failed.exchange";

    private String failedQueue = "mall.coupon.failed.queue";

    private String failedRoutingKey = "mall.coupon.failed.routing-key";

    private Integer timeoutMinutes = 30;


    public String getCouponRewardExchange() {
        return couponRewardExchange;
    }

    public void setCouponRewardExchange(String couponRewardExchange) {
        this.couponRewardExchange = couponRewardExchange;
    }

    public String getCouponRewardQueue() {
        return couponRewardQueue;
    }

    public void setCouponRewardQueue(String couponRewardQueue) {
        this.couponRewardQueue = couponRewardQueue;
    }

    public String getCouponRewardRoutingKey() {
        return couponRewardRoutingKey;
    }

    public void setCouponRewardRoutingKey(String couponRewardRoutingKey) {
        this.couponRewardRoutingKey = couponRewardRoutingKey;
    }

    public String getFailedExchange() {
        return failedExchange;
    }

    public void setFailedExchange(String failedExchange) {
        this.failedExchange = failedExchange;
    }

    public String getFailedQueue() {
        return failedQueue;
    }

    public void setFailedQueue(String failedQueue) {
        this.failedQueue = failedQueue;
    }

    public String getFailedRoutingKey() {
        return failedRoutingKey;
    }

    public void setFailedRoutingKey(String failedRoutingKey) {
        this.failedRoutingKey = failedRoutingKey;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }
}
