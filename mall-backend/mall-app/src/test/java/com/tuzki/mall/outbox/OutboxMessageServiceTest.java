package com.tuzki.mall.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.outbox.entity.OutboxMessage;
import com.tuzki.mall.outbox.enums.OutboxMessageStatus;
import com.tuzki.mall.outbox.mapper.OutboxMessageMapper;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.outbox.service.OutboxRabbitPublisher;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox 消息服务测试，验证消息先落库再投递以及失败后可保留等待补偿。
 */
class OutboxMessageServiceTest {

    @Test
    void createAndPublishMarksMessageSentWhenRabbitPublishSucceeds() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxRabbitPublisher publisher = mock(OutboxRabbitPublisher.class);
        OutboxMessageService service = new OutboxMessageService(mapper, publisher, new ObjectMapper());
        CartChangeMessage payload = new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT);

        service.createAndPublish("CART", "1:2:4", "mall.cart.change.exchange",
                "mall.cart.change.routing-key", payload);

        verify(mapper).insert(any(OutboxMessage.class));
        verify(publisher).publish("mall.cart.change.exchange", "mall.cart.change.routing-key", payload);
        verify(mapper).markSent(any(String.class));
    }

    @Test
    void createAndPublishKeepsMessageRetryableWhenRabbitPublishFails() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxRabbitPublisher publisher = mock(OutboxRabbitPublisher.class);
        doThrow(new IllegalStateException("rabbit unavailable")).when(publisher)
                .publish(any(String.class), any(String.class), any());
        OutboxMessageService service = new OutboxMessageService(mapper, publisher, new ObjectMapper());

        service.createAndPublish("CART", "1:2:4", "mall.cart.change.exchange",
                "mall.cart.change.routing-key",
                new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT));

        verify(mapper).insert(any(OutboxMessage.class));
        verify(mapper).markFailed(any(String.class), any(LocalDateTime.class), any(String.class));
    }

    @Test
    void relayPendingMessagesPublishesDueMessages() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxRabbitPublisher publisher = mock(OutboxRabbitPublisher.class);
        OutboxMessage message = pendingCartMessage();
        when(mapper.listDueMessages(any(LocalDateTime.class), any(Integer.class))).thenReturn(List.of(message));
        OutboxMessageService service = new OutboxMessageService(mapper, publisher, new ObjectMapper());

        service.relayDueMessages(100);

        verify(publisher).publish("mall.cart.change.exchange", "mall.cart.change.routing-key",
                new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT));
        verify(mapper).markSent("msg-1");
    }

    @Test
    void relayPendingMessagesLeavesFailedMessageForNextRetry() {
        OutboxMessageMapper mapper = mock(OutboxMessageMapper.class);
        OutboxRabbitPublisher publisher = mock(OutboxRabbitPublisher.class);
        OutboxMessage message = pendingCartMessage();
        when(mapper.listDueMessages(any(LocalDateTime.class), any(Integer.class))).thenReturn(List.of(message));
        doThrow(new IllegalStateException("rabbit unavailable")).when(publisher)
                .publish(any(String.class), any(String.class), any());
        OutboxMessageService service = new OutboxMessageService(mapper, publisher, new ObjectMapper());

        service.relayDueMessages(100);

        verify(mapper).markFailed(any(String.class), any(LocalDateTime.class), any(String.class));
    }

    private OutboxMessage pendingCartMessage() {
        OutboxMessage message = new OutboxMessage();
        message.setMessageId("msg-1");
        message.setAggregateType("CART");
        message.setAggregateId("1:2:4");
        message.setExchangeName("mall.cart.change.exchange");
        message.setRoutingKey("mall.cart.change.routing-key");
        message.setPayloadType(CartChangeMessage.class.getName());
        message.setPayload("""
                {"userId":1,"skuId":2,"quantity":3,"version":4,"operation":"UPSERT"}
                """);
        message.setStatus(OutboxMessageStatus.PENDING.getCode());
        return message;
    }

    @Test
    void statusCodesStayStableForDatabaseRows() {
        assertThat(OutboxMessageStatus.PENDING.getCode()).isZero();
        assertThat(OutboxMessageStatus.SENT.getCode()).isEqualTo(1);
        assertThat(OutboxMessageStatus.FAILED.getCode()).isEqualTo(2);
    }
}
