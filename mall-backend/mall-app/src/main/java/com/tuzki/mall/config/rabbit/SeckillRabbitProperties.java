package com.tuzki.mall.config.rabbit;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requireText;

/**
 * 秒杀 RabbitMQ 配置属性，集中维护异步下单交换机、队列、路由键和失败队列配置。
 */
@ConfigurationProperties(prefix = "mall.seckill.rabbit")
public class SeckillRabbitProperties {

    private String orderExchange = "mall.seckill.order.exchange";

    private String orderQueue = "mall.seckill.order.queue";

    private String orderRoutingKey = "mall.seckill.order.routing-key";

    private String failedExchange = "mall.seckill.failed.exchange";

    private String failedQueue = "mall.seckill.failed.queue";

    private String failedRoutingKey = "mall.seckill.failed.routing-key";

    @PostConstruct
    public void validate() {
        requireText(orderExchange, "秒杀下单交换机不能为空");
        requireText(orderQueue, "秒杀下单队列不能为空");
        requireText(orderRoutingKey, "秒杀下单路由键不能为空");
        requireText(failedExchange, "秒杀失败交换机不能为空");
        requireText(failedQueue, "秒杀失败队列不能为空");
        requireText(failedRoutingKey, "秒杀失败路由键不能为空");
    }

    public String getOrderExchange() {
        return orderExchange;
    }

    public void setOrderExchange(String orderExchange) {
        this.orderExchange = orderExchange;
    }

    public String getOrderQueue() {
        return orderQueue;
    }

    public void setOrderQueue(String orderQueue) {
        this.orderQueue = orderQueue;
    }

    public String getOrderRoutingKey() {
        return orderRoutingKey;
    }

    public void setOrderRoutingKey(String orderRoutingKey) {
        this.orderRoutingKey = orderRoutingKey;
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
}
