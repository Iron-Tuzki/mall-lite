package com.tuzki.mall.cart.service;

import com.tuzki.mall.cart.mapper.CartItemMapper;
import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import org.springframework.stereotype.Service;

/**
 * 购物车持久化服务，消费逐项变更消息并按版本号异步同步 MySQL。
 */
@Service
public class CartPersistenceService {

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private final CartItemMapper cartItemMapper;

    public CartPersistenceService(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    /**
     * 将购物车变更应用到 MySQL，旧版本消息和重复消息不会覆盖新状态。
     *
     * @param message 购物车逐项变更消息
     */
    public void apply(CartChangeMessage message) {
        boolean delete = message.operation() == CartChangeOperation.DELETE;
        cartItemMapper.upsertIfNewer(
                message.userId(),
                message.skuId(),
                delete ? 0 : message.quantity(),
                message.version(),
                delete ? DELETED : NOT_DELETED
        );
    }
}
