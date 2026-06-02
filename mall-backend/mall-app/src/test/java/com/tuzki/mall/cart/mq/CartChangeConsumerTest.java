package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.cart.service.CartPersistenceService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 购物车变更消息消费者测试，验证消费端会将消息交给持久化服务。
 */
class CartChangeConsumerTest {

    @Test
    void consumedMessageIsAppliedToMysqlPersistence() {
        CartPersistenceService cartPersistenceService = mock(CartPersistenceService.class);
        CartChangeMessage message = new CartChangeMessage(1L, 2L, 3, 4L, CartChangeOperation.UPSERT);

        new CartChangeConsumer(cartPersistenceService).handle(message);

        verify(cartPersistenceService).apply(message);
    }
}
