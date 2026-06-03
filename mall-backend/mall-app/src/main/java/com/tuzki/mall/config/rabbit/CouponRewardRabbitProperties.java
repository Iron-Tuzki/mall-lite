package com.tuzki.mall.config.rabbit;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requirePositive;
import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requireText;

/**
 * 优惠券 RabbitMQ 配置属性，集中维护相关的交换机、队列、路由键和超时时间。
 */
@ConfigurationProperties(prefix = "mall.coupon.rabbit")
public class CouponRewardRabbitProperties {

    private String couponRewardExchange = "mall.coupon-reward.exchange";

    private String couponRewardQueue = "mall.coupon-reward.queue";

    private String couponRewardRoutingKey = "mall.coupon-reward.routing-key";

    private String failedExchange = "mall.coupon.failed.exchange";

    private String failedQueue = "mall.coupon.failed.queue";

    private String failedRoutingKey = "mall.coupon.failed.routing-key";

    private Integer timeoutMinutes = 30;

    /**
     * 校验优惠券奖励 RabbitMQ 配置，避免空交换机、空队列、空路由键或非法超时时间进入运行期。
     */
    @PostConstruct
    public void validate() {
        requireText(couponRewardExchange, "优惠券奖励交换机不能为空");
        requireText(couponRewardQueue, "优惠券奖励队列不能为空");
        requireText(couponRewardRoutingKey, "优惠券奖励路由键不能为空");
        requireText(failedExchange, "优惠券失败交换机不能为空");
        requireText(failedQueue, "优惠券失败队列不能为空");
        requireText(failedRoutingKey, "优惠券失败路由键不能为空");
        requirePositive(timeoutMinutes, "优惠券奖励超时时间必须大于 0");
    }

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
