package com.tuzki.mall.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.cart.entity.CartItem;
import com.tuzki.mall.cart.mapper.CartItemMapper;
import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.cart.service.CartPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物车异步持久化集成测试，验证版本号可以抵御重复消息和乱序消息。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class CartPersistenceServiceIntegrationTest {

    @Autowired
    private CartPersistenceService cartPersistenceService;

    @Autowired
    private CartItemMapper cartItemMapper;

    @AfterEach
    void clearCartItem() {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, TestSeedData.USER_ID)
                .eq(CartItem::getSkuId, TestSeedData.SKU_ID));
    }

    @Test
    void newerVersionWinsWhenMessagesArriveOutOfOrder() {
        cartPersistenceService.apply(message(2, 1L, CartChangeOperation.UPSERT));
        cartPersistenceService.apply(message(5, 3L, CartChangeOperation.UPSERT));
        cartPersistenceService.apply(message(3, 2L, CartChangeOperation.UPSERT));

        CartItem item = findCartItem();
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getVersion()).isEqualTo(3L);
        assertThat(item.getDeleted()).isZero();
    }

    @Test
    void deleteTombstoneCannotBeRevivedByOlderUpdate() {
        cartPersistenceService.apply(message(5, 3L, CartChangeOperation.UPSERT));
        cartPersistenceService.apply(message(0, 4L, CartChangeOperation.DELETE));
        cartPersistenceService.apply(message(8, 3L, CartChangeOperation.UPSERT));

        CartItem item = findCartItem();
        assertThat(item.getQuantity()).isZero();
        assertThat(item.getVersion()).isEqualTo(4L);
        assertThat(item.getDeleted()).isEqualTo(1);
    }

    private CartChangeMessage message(Integer quantity, Long version, CartChangeOperation operation) {
        return new CartChangeMessage(TestSeedData.USER_ID, TestSeedData.SKU_ID, quantity, version, operation);
    }

    private CartItem findCartItem() {
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, TestSeedData.USER_ID)
                .eq(CartItem::getSkuId, TestSeedData.SKU_ID));
    }
}
