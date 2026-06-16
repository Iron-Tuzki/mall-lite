package com.tuzki.mall.outbox.service;

import com.tuzki.mall.common.exception.BusinessException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 RabbitTemplate 的 Outbox 消息投递器，负责将还原后的消息对象发送到 RabbitMQ。
 */
@Component
public class RabbitTemplateOutboxPublisher implements OutboxRabbitPublisher {

    private static final long CONFIRM_TIMEOUT_MILLIS = 3_000L;

    private final RabbitTemplate rabbitTemplate;

    public RabbitTemplateOutboxPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String exchangeName, String routingKey, Object payload) {
        try {
            boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(exchangeName, routingKey, payload);
                return operations.waitForConfirms(CONFIRM_TIMEOUT_MILLIS);
            });
            if (!confirmed) {
                throw new BusinessException(503, "outbox message publish failed");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(503, "outbox message publish failed");
        }
    }
}
