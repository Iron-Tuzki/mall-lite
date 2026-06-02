package com.tuzki.mall.cart.mq;

import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.service.CartPersistenceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 购物车变更消息消费者，负责将 Redis 实时变更异步同步到 MySQL。
 */
@Component
public class CartChangeConsumer {

    private final CartPersistenceService cartPersistenceService;

    public CartChangeConsumer(CartPersistenceService cartPersistenceService) {
        this.cartPersistenceService = cartPersistenceService;
    }

    /**
     * 消费购物车逐项变更消息。
     *
     * @param message 购物车逐项变更消息
     */
    @RabbitListener(queues = "${mall.cart.rabbit.change-queue}")
    public void handle(CartChangeMessage message) {
        cartPersistenceService.apply(message);
    }
}
