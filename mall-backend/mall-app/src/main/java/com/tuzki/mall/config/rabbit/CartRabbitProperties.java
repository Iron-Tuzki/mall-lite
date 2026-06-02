package com.tuzki.mall.config.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 购物车 RabbitMQ 配置属性，集中维护变更交换机、队列、路由键和 Confirm 超时时间。
 */
@ConfigurationProperties(prefix = "mall.cart.rabbit")
public class CartRabbitProperties {

    private String changeExchange = "mall.cart.change.exchange";
    private String changeQueue = "mall.cart.change.queue";
    private String changeRoutingKey = "mall.cart.change.routing-key";
    private Integer confirmTimeoutSeconds = 3;

    public String getChangeExchange() { return changeExchange; }
    public void setChangeExchange(String changeExchange) { this.changeExchange = changeExchange; }
    public String getChangeQueue() { return changeQueue; }
    public void setChangeQueue(String changeQueue) { this.changeQueue = changeQueue; }
    public String getChangeRoutingKey() { return changeRoutingKey; }
    public void setChangeRoutingKey(String changeRoutingKey) { this.changeRoutingKey = changeRoutingKey; }
    public Integer getConfirmTimeoutSeconds() { return confirmTimeoutSeconds; }
    public void setConfirmTimeoutSeconds(Integer confirmTimeoutSeconds) { this.confirmTimeoutSeconds = confirmTimeoutSeconds; }
}
