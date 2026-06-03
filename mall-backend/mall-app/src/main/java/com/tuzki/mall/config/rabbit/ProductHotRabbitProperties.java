package com.tuzki.mall.config.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 商品热点 RabbitMQ 配置属性，集中维护热点事件交换机、队列、路由键和 Confirm 超时时间。
 */
@ConfigurationProperties(prefix = "mall.product.hot-rabbit")
public class ProductHotRabbitProperties {

    private String eventExchange = "mall.product.hot.event.exchange";

    private String eventQueue = "mall.product.hot.event.queue";

    private String eventRoutingKey = "mall.product.hot.event.routing-key";

    private String failedExchange = "mall.product.hot.failed.exchange";

    private String failedQueue = "mall.product.hot.failed.queue";

    private String failedRoutingKey = "mall.product.hot.failed.routing-key";

    private Integer confirmTimeoutSeconds = 3;

    public String getEventExchange() {
        return eventExchange;
    }

    public void setEventExchange(String eventExchange) {
        this.eventExchange = eventExchange;
    }

    public String getEventQueue() {
        return eventQueue;
    }

    public void setEventQueue(String eventQueue) {
        this.eventQueue = eventQueue;
    }

    public String getEventRoutingKey() {
        return eventRoutingKey;
    }

    public void setEventRoutingKey(String eventRoutingKey) {
        this.eventRoutingKey = eventRoutingKey;
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

    public Integer getConfirmTimeoutSeconds() {
        return confirmTimeoutSeconds;
    }

    public void setConfirmTimeoutSeconds(Integer confirmTimeoutSeconds) {
        this.confirmTimeoutSeconds = confirmTimeoutSeconds;
    }
}
