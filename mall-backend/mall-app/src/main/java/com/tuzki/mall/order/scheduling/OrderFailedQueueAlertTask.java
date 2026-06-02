package com.tuzki.mall.order.scheduling;

import com.tuzki.mall.config.rabbit.OrderRabbitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单取消失败队列告警任务，定期检查失败消息积压并输出结构化错误日志。
 */
@Component
public class OrderFailedQueueAlertTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderFailedQueueAlertTask.class);

    private final AmqpAdmin amqpAdmin;

    private final OrderRabbitProperties properties;

    public OrderFailedQueueAlertTask(AmqpAdmin amqpAdmin, OrderRabbitProperties properties) {
        this.amqpAdmin = amqpAdmin;
        this.properties = properties;
    }

    /**
     * 检查订单取消失败队列，存在积压消息时输出错误日志。
     */
    @Scheduled(
            initialDelayString = "${mall.order.scheduling.alert-fixed-delay-ms:60000}",
            fixedDelayString = "${mall.order.scheduling.alert-fixed-delay-ms:60000}"
    )
    public void alertWhenFailedMessagesExist() {
        QueueInformation queueInformation = amqpAdmin.getQueueInfo(properties.getFailedQueue());
        if (queueInformation != null && queueInformation.getMessageCount() > 0) {
            LOGGER.error("order timeout failed queue has pending messages, queue={}, messageCount={}",
                    properties.getFailedQueue(), queueInformation.getMessageCount());
        }
    }
}
