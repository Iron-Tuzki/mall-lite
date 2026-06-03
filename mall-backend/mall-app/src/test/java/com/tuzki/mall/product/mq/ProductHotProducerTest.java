package com.tuzki.mall.product.mq;

import com.tuzki.mall.config.rabbit.ProductHotRabbitProperties;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品热点事件生产者测试，验证消息发送后等待 Broker Confirm。
 */
class ProductHotProducerTest {

    @Test
    void confirmedMessageIsSentToConfiguredExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        ProductHotRabbitProperties properties = new ProductHotRabbitProperties();
        ProductHotEvent event = new ProductHotEvent("event-1", 100L, ProductHotAction.VIEW, LocalDateTime.now());
        when(operations.waitForConfirms(3_000L)).thenReturn(true);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<RabbitOperations.OperationsCallback<Boolean>>getArgument(0).doInRabbit(operations));

        new ProductHotProducer(rabbitTemplate, properties).send(event);

        verify(operations).convertAndSend(properties.getEventExchange(), properties.getEventRoutingKey(), event);
        verify(operations).waitForConfirms(3_000L);
    }
}
