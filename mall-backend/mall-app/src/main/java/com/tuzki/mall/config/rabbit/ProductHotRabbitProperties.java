package com.tuzki.mall.config.rabbit;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requirePositive;
import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requireText;

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

    /**
     * 校验商品热点 RabbitMQ 配置，避免空交换机、空队列、空路由键或非法 Confirm 超时时间进入运行期。
     */
    @PostConstruct
    public void validate() {
        requireText(eventExchange, "商品热点事件交换机不能为空");
        requireText(eventQueue, "商品热点事件队列不能为空");
        requireText(eventRoutingKey, "商品热点事件路由键不能为空");
        requireText(failedExchange, "商品热点失败交换机不能为空");
        requireText(failedQueue, "商品热点失败队列不能为空");
        requireText(failedRoutingKey, "商品热点失败路由键不能为空");
        requirePositive(confirmTimeoutSeconds, "商品热点消息 Confirm 超时时间必须大于 0");
    }

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
