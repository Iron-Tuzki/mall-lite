package com.tuzki.mall.cart;

import com.tuzki.mall.cart.dto.CartAddRequest;
import com.tuzki.mall.cart.mapper.CartItemMapper;
import com.tuzki.mall.cart.message.CartChangeMessageSender;
import com.tuzki.mall.cart.service.CartCacheItem;
import com.tuzki.mall.cart.service.CartCacheMutation;
import com.tuzki.mall.cart.service.CartCacheService;
import com.tuzki.mall.cart.service.impl.CartServiceImpl;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 购物车服务测试，验证加入购物车成功后会发送商品热点事件。
 */
class CartServiceImplTest {

    @Test
    void successfulCartAddSendsProductHotEvent() {
        CartCacheService cartCacheService = mock(CartCacheService.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductHotEventSender productHotEventSender = mock(ProductHotEventSender.class);
        CartAddRequest request = new CartAddRequest();
        request.setSkuId(10L);
        request.setQuantity(2);
        when(cartCacheService.isLoaded(1L)).thenReturn(true);
        when(skuMapper.selectOne(any())).thenReturn(activeSku(10L, 100L));
        when(productMapper.selectOne(any())).thenReturn(activeProduct(100L));
        when(cartCacheService.add(1L, 10L, 2)).thenReturn(new CartCacheMutation(
                null,
                new CartCacheItem(2, 1L, false)));

        new CartServiceImpl(
                cartCacheService,
                mock(CartItemMapper.class),
                skuMapper,
                productMapper,
                mock(CartChangeMessageSender.class),
                productHotEventSender)
                .add(1L, request);

        verify(productHotEventSender).send(argThat(event ->
                event.productId().equals(100L) && event.action() == ProductHotAction.CART_ADD));
    }

    private Sku activeSku(Long skuId, Long productId) {
        Sku sku = new Sku();
        sku.setId(skuId);
        sku.setProductId(productId);
        sku.setStatus(1);
        sku.setDeleted(0);
        return sku;
    }

    private Product activeProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }
}
