package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.config.rabbit.CartRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 购物车 Outbox 消息发送器测试，验证购物车变更先写入 Outbox 再尝试投递 RabbitMQ。
 */
class CartOutboxChangeMessageSenderTest {

    @Test
    void sendCreatesCartOutboxMessageUsingConfiguredRabbitDestination() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        CartRabbitProperties properties = new CartRabbitProperties();
        properties.setChangeExchange("mall.cart.change.exchange");
        properties.setChangeRoutingKey("mall.cart.change.routing-key");
        CartChangeMessage message = new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT);

        new CartOutboxChangeMessageSender(outboxMessageService, properties).send(message);

        verify(outboxMessageService).createAndPublish(
                "CART",
                "1:2:4",
                "mall.cart.change.exchange",
                "mall.cart.change.routing-key",
                message
        );
    }
}
