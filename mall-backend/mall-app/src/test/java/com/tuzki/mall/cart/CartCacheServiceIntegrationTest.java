package com.tuzki.mall.cart;

import com.tuzki.mall.TestSeedData;
import com.tuzki.mall.cart.entity.CartItem;
import com.tuzki.mall.cart.service.CartCacheMutation;
import com.tuzki.mall.cart.service.CartCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 购物车 Redis 缓存集成测试，验证原子变更、删除墓碑、条件回滚和数据库回源重建。
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class CartCacheServiceIntegrationTest {

    private static final String CART_KEY = "mall:cart:user:" + TestSeedData.USER_ID;

    private static final String LOADED_KEY = "mall:cart:loaded:" + TestSeedData.USER_ID;

    @Autowired
    private CartCacheService cartCacheService;

    @Autowired
    private RedissonClient redissonClient;

    @AfterEach
    void clearCache() {
        redissonClient.getKeys().delete(CART_KEY, LOADED_KEY);
    }

    @Test
    void mutationKeepsDeleteTombstoneAndMonotonicVersion() {
        CartCacheMutation first = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 2, false);
        CartCacheMutation second = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 5, false);
        CartCacheMutation deleted = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 0, true);
        CartCacheMutation restored = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 1, false);

        assertThat(first.current().quantity()).isEqualTo(2);
        assertThat(first.current().version()).isEqualTo(1L);
        assertThat(second.current().version()).isEqualTo(2L);
        assertThat(deleted.current().deleted()).isTrue();
        assertThat(deleted.current().version()).isEqualTo(3L);
        assertThat(restored.current().quantity()).isEqualTo(1);
        assertThat(restored.current().version()).isEqualTo(4L);
    }

    @Test
    void rollbackOnlyRestoresCurrentFailedVersion() {
        CartCacheMutation first = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 2, false);
        CartCacheMutation second = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 5, false);

        assertThat(cartCacheService.rollback(TestSeedData.USER_ID, TestSeedData.SKU_ID,
                second.current().version(), second.previousJson())).isTrue();
        assertThat(cartCacheService.getAll(TestSeedData.USER_ID).get(TestSeedData.SKU_ID).quantity()).isEqualTo(2);

        CartCacheMutation third = cartCacheService.mutate(TestSeedData.USER_ID, TestSeedData.SKU_ID, 8, false);
        assertThat(cartCacheService.rollback(TestSeedData.USER_ID, TestSeedData.SKU_ID,
                first.current().version(), first.previousJson())).isFalse();
        assertThat(cartCacheService.getAll(TestSeedData.USER_ID).get(TestSeedData.SKU_ID).quantity()).isEqualTo(8);
        assertThat(third.current().version()).isEqualTo(2L);
    }

    @Test
    void rebuildLoadsActiveItemsAndDeleteTombstones() {
        CartItem active = item(TestSeedData.SKU_ID, 2, 3L, 0);
        CartItem deleted = item(TestSeedData.SKU_ID_2, 0, 4L, 1);

        cartCacheService.rebuild(TestSeedData.USER_ID, List.of(active, deleted));

        assertThat(cartCacheService.isLoaded(TestSeedData.USER_ID)).isTrue();
        assertThat(cartCacheService.getAll(TestSeedData.USER_ID)).hasSize(2);
        assertThat(cartCacheService.getAll(TestSeedData.USER_ID).get(TestSeedData.SKU_ID_2).deleted()).isTrue();
    }

    private CartItem item(Long skuId, Integer quantity, Long version, Integer deleted) {
        CartItem item = new CartItem();
        item.setUserId(TestSeedData.USER_ID);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setVersion(version);
        item.setDeleted(deleted);
        return item;
    }
}
