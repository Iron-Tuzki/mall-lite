package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.config.rabbit.CartRabbitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车变更消息生产者测试，验证消息发送后等待 Broker Confirm。
 */
class CartChangeProducerTest {

    @Test
    void confirmedMessageIsSentToConfiguredExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        CartRabbitProperties properties = properties();
        when(operations.waitForConfirms(3_000L)).thenReturn(true);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<RabbitOperations.OperationsCallback<Boolean>>getArgument(0).doInRabbit(operations));

        new CartChangeProducer(rabbitTemplate, properties).send(message());

        verify(operations).convertAndSend(properties.getChangeExchange(), properties.getChangeRoutingKey(), message());
        verify(operations).waitForConfirms(3_000L);
    }

    @Test
    void negativeConfirmIsReportedAsBusinessFailure() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(operations.waitForConfirms(3_000L)).thenReturn(false);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<RabbitOperations.OperationsCallback<Boolean>>getArgument(0).doInRabbit(operations));

        assertThatThrownBy(() -> new CartChangeProducer(rabbitTemplate, properties()).send(message()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cart change message send failed");
    }

    private CartRabbitProperties properties() {
        return new CartRabbitProperties();
    }

    private CartChangeMessage message() {
        return new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT);
    }
}
