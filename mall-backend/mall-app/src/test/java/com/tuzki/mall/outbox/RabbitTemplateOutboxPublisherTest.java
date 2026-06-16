package com.tuzki.mall.outbox;

import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.outbox.service.RabbitTemplateOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RabbitTemplate Outbox 投递器测试，验证消息投递需要等待 Broker Confirm。
 */
class RabbitTemplateOutboxPublisherTest {

    @Test
    void publishWaitsForBrokerConfirmBeforeReturning() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(operations.waitForConfirms(3_000L)).thenReturn(true);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<RabbitOperations.OperationsCallback<Boolean>>getArgument(0).doInRabbit(operations));

        new RabbitTemplateOutboxPublisher(rabbitTemplate).publish("exchange", "routing-key", "payload");

        verify(operations).convertAndSend("exchange", "routing-key", "payload");
        verify(operations).waitForConfirms(3_000L);
    }

    @Test
    void negativeConfirmIsReportedAsPublishFailure() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitOperations operations = mock(RabbitOperations.class);
        when(operations.waitForConfirms(3_000L)).thenReturn(false);
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation ->
                invocation.<RabbitOperations.OperationsCallback<Boolean>>getArgument(0).doInRabbit(operations));

        assertThatThrownBy(() -> new RabbitTemplateOutboxPublisher(rabbitTemplate)
                .publish("exchange", "routing-key", "payload"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("outbox message publish failed");
    }
}
