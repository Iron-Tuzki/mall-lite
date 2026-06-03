package com.tuzki.mall.product.mq;

import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.config.rabbit.ProductHotRabbitProperties;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 商品热点事件生产者，负责投递热点事件消息并等待 Broker Confirm。
 */
@Component
public class ProductHotProducer implements ProductHotEventSender {

    private final RabbitTemplate rabbitTemplate;

    private final ProductHotRabbitProperties properties;

    public ProductHotProducer(RabbitTemplate rabbitTemplate, ProductHotRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * 发送商品热点事件消息。
     *
     * @param event 商品热点事件，包含事件 ID、商品 ID、行为类型和发生时间
     */
    @Override
    public void send(ProductHotEvent event) {
        try {
            boolean confirmed = rabbitTemplate.invoke(operations -> {
                operations.convertAndSend(properties.getEventExchange(), properties.getEventRoutingKey(), event);
                return operations.waitForConfirms(properties.getConfirmTimeoutSeconds() * 1_000L);
            });
            if (!confirmed) {
                throw new BusinessException(503, "product hot event message send failed");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(503, "product hot event message send failed");
        }
    }
}
