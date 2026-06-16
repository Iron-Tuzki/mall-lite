package com.tuzki.mall.outbox.service;

/**
 * Outbox RabbitMQ 投递接口，屏蔽具体 RabbitTemplate 调用细节。
 */
public interface OutboxRabbitPublisher {

    /**
     * 投递 Outbox 消息到指定 RabbitMQ 交换机。
     *
     * @param exchangeName 目标交换机名称
     * @param routingKey 目标路由键
     * @param payload 消息体对象
     */
    void publish(String exchangeName, String routingKey, Object payload);
}
