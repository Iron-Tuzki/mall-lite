package com.tuzki.mall.product;

import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import com.tuzki.mall.product.mapper.ProductFavoriteMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.service.ProductFavoriteCacheService;
import com.tuzki.mall.product.service.impl.ProductFavoriteServiceImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品收藏服务测试，验证收藏成功后会发送商品热点事件。
 */
class ProductFavoriteServiceImplTest {

    @Test
    void successfulFavoriteSendsProductHotEvent() {
        ProductFavoriteMapper productFavoriteMapper = mock(ProductFavoriteMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductHotEventSender productHotEventSender = mock(ProductHotEventSender.class);
        when(productMapper.selectOne(any())).thenReturn(activeProduct(100L));

        new ProductFavoriteServiceImpl(
                productFavoriteMapper,
                productMapper,
                mock(ProductFavoriteCacheService.class),
                productHotEventSender)
                .favorite(1L, 100L);

        verify(productHotEventSender).send(argThat(event ->
                event.productId().equals(100L) && event.action() == ProductHotAction.FAVORITE));
    }

    private Product activeProduct(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }
}
