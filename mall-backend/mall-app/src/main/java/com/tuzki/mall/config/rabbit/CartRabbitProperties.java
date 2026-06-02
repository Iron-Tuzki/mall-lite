package com.tuzki.mall.config.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 购物车 RabbitMQ 配置属性，集中维护变更交换机、失败交换机、队列、路由键和 Confirm 超时时间。
 */
@ConfigurationProperties(prefix = "mall.cart.rabbit")
public class CartRabbitProperties {

    private String changeExchange = "mall.cart.change.exchange";
    private String changeQueue = "mall.cart.change.queue";
    private String changeRoutingKey = "mall.cart.change.routing-key";
    private String failedExchange = "mall.cart.failed.exchange";
    private String failedQueue = "mall.cart.failed.queue";
    private String failedRoutingKey = "mall.cart.failed.routing-key";
    private Integer confirmTimeoutSeconds = 3;

    public String getChangeExchange() { return changeExchange; }
    public void setChangeExchange(String changeExchange) { this.changeExchange = changeExchange; }
    public String getChangeQueue() { return changeQueue; }
    public void setChangeQueue(String changeQueue) { this.changeQueue = changeQueue; }
    public String getChangeRoutingKey() { return changeRoutingKey; }
    public void setChangeRoutingKey(String changeRoutingKey) { this.changeRoutingKey = changeRoutingKey; }
    public String getFailedExchange() { return failedExchange; }
    public void setFailedExchange(String failedExchange) { this.failedExchange = failedExchange; }
    public String getFailedQueue() { return failedQueue; }
    public void setFailedQueue(String failedQueue) { this.failedQueue = failedQueue; }
    public String getFailedRoutingKey() { return failedRoutingKey; }
    public void setFailedRoutingKey(String failedRoutingKey) { this.failedRoutingKey = failedRoutingKey; }
    public Integer getConfirmTimeoutSeconds() { return confirmTimeoutSeconds; }
    public void setConfirmTimeoutSeconds(Integer confirmTimeoutSeconds) { this.confirmTimeoutSeconds = confirmTimeoutSeconds; }
}
