package com.tuzki.mall.config.rabbit;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requirePositive;
import static com.tuzki.mall.config.rabbit.RabbitPropertiesValidationSupport.requireText;

/**
 * 订单 RabbitMQ 配置属性，集中维护订单超时取消相关的交换机、队列、路由键和超时时间。
 */
@ConfigurationProperties(prefix = "mall.order.rabbit")
public class OrderRabbitProperties {

    private String delayExchange = "mall.order.delay.exchange";

    private String delayQueue = "mall.order.delay.queue";

    private String delayRoutingKey = "mall.order.delay.routing-key";

    private String cancelExchange = "mall.order.cancel.exchange";

    private String cancelQueue = "mall.order.cancel.queue";

    private String cancelRoutingKey = "mall.order.cancel.routing-key";

    private String failedExchange = "mall.order.failed.exchange";

    private String failedQueue = "mall.order.failed.queue";

    private String failedRoutingKey = "mall.order.failed.routing-key";

    private Integer timeoutMinutes = 30;

    /**
     * 校验订单 RabbitMQ 配置，避免空交换机、空队列、空路由键或非法超时时间进入运行期。
     */
    @PostConstruct
    public void validate() {
        requireText(delayExchange, "订单延迟交换机不能为空");
        requireText(delayQueue, "订单延迟队列不能为空");
        requireText(delayRoutingKey, "订单延迟路由键不能为空");
        requireText(cancelExchange, "订单取消交换机不能为空");
        requireText(cancelQueue, "订单取消队列不能为空");
        requireText(cancelRoutingKey, "订单取消路由键不能为空");
        requireText(failedExchange, "订单失败交换机不能为空");
        requireText(failedQueue, "订单失败队列不能为空");
        requireText(failedRoutingKey, "订单失败路由键不能为空");
        requirePositive(timeoutMinutes, "订单超时时间必须大于 0");
    }

    public String getDelayExchange() {
        return delayExchange;
    }

    public void setDelayExchange(String delayExchange) {
        this.delayExchange = delayExchange;
    }

    public String getDelayQueue() {
        return delayQueue;
    }

    public void setDelayQueue(String delayQueue) {
        this.delayQueue = delayQueue;
    }

    public String getDelayRoutingKey() {
        return delayRoutingKey;
    }

    public void setDelayRoutingKey(String delayRoutingKey) {
        this.delayRoutingKey = delayRoutingKey;
    }

    public String getCancelExchange() {
        return cancelExchange;
    }

    public void setCancelExchange(String cancelExchange) {
        this.cancelExchange = cancelExchange;
    }

    public String getCancelQueue() {
        return cancelQueue;
    }

    public void setCancelQueue(String cancelQueue) {
        this.cancelQueue = cancelQueue;
    }

    public String getCancelRoutingKey() {
        return cancelRoutingKey;
    }

    public void setCancelRoutingKey(String cancelRoutingKey) {
        this.cancelRoutingKey = cancelRoutingKey;
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
