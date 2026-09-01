package com.tuzki.mall.config.rabbit;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requirePositive;
import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requireText;

/**
 * 商品搜索索引 RabbitMQ 配置属性，集中维护事件交换机、队列、路由键和失败队列信息。
 */
@ConfigurationProperties(prefix = "mall.product.search-index-rabbit")
public class ProductSearchIndexRabbitProperties {

    private String eventExchange = "mall.product.search-index.event.exchange";

    private String eventQueue = "mall.product.search-index.event.queue";

    private String eventRoutingKey = "mall.product.search-index.event.routing-key";

    private String failedExchange = "mall.product.search-index.failed.exchange";

    private String failedQueue = "mall.product.search-index.failed.queue";

    private String failedRoutingKey = "mall.product.search-index.failed.routing-key";

    private Integer confirmTimeoutSeconds = 3;

    /**
     * 校验商品搜索索引 RabbitMQ 配置，避免空交换机、空队列、空路由键或非法 Confirm 超时时间进入运行期。
     */
    @PostConstruct
    public void validate() {
        requireText(eventExchange, "商品搜索索引事件交换机不能为空");
        requireText(eventQueue, "商品搜索索引事件队列不能为空");
        requireText(eventRoutingKey, "商品搜索索引事件路由键不能为空");
        requireText(failedExchange, "商品搜索索引失败交换机不能为空");
        requireText(failedQueue, "商品搜索索引失败队列不能为空");
        requireText(failedRoutingKey, "商品搜索索引失败路由键不能为空");
        requirePositive(confirmTimeoutSeconds, "商品搜索索引消息 Confirm 超时时间必须大于 0");
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
