package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeMessageSender;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.config.rabbit.CartRabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 购物车变更消息生产者，负责投递逐项变更消息并等待 Broker Confirm。
 */
@Component
public class CartChangeProducer implements CartChangeMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final CartRabbitProperties properties;

    public CartChangeProducer(RabbitTemplate rabbitTemplate, CartRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * 发送购物车逐项变更消息，并同步等待 Broker Confirm。
     *
     * @param message 购物车变更消息
     */
    @Override
    public void send(CartChangeMessage message) {
        try {
            boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(properties.getChangeExchange(), properties.getChangeRoutingKey(), message);
                return operations.waitForConfirms(properties.getConfirmTimeoutSeconds() * 1_000L);
            });
            if (!confirmed) {
                throw new BusinessException(503, "cart change message send failed");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(503, "cart change message send failed");
        }
    }
}
