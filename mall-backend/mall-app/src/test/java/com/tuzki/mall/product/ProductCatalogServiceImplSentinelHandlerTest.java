package com.tuzki.mall.product;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.sentinel.ProductHotSentinelResources;
import com.tuzki.mall.product.service.ProductDetailCacheService;
import com.tuzki.mall.product.service.ProductHotDetailCacheService;
import com.tuzki.mall.product.service.impl.ProductCatalogServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 商品目录服务 Sentinel 处理器测试，验证限流和熔断降级返回不同业务语义。
 */
class ProductCatalogServiceImplSentinelHandlerTest {

    @Test
    void flowControlBlockReturnsBusyException() {
        ProductCatalogServiceImpl service = service();

        assertThatThrownBy(() -> service.handleHotProductDetailBlocked(
                1001L, new FlowException(ProductHotSentinelResources.HOT_PRODUCT_DETAIL)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("hot product detail is busy")
                .extracting("code")
                .isEqualTo(429);
    }

    @Test
    void circuitBreakerBlockReturnsDegradedException() {
        ProductCatalogServiceImpl service = service();

        assertThatThrownBy(() -> service.handleHotProductDetailBlocked(
                1001L, new DegradeException(ProductHotSentinelResources.HOT_PRODUCT_DETAIL)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("hot product detail degraded")
                .extracting("code")
                .isEqualTo(503);
    }

    private ProductCatalogServiceImpl service() {
        return new ProductCatalogServiceImpl(
                mock(CategoryMapper.class),
                mock(ProductMapper.class),
                mock(SkuMapper.class),
                mock(ProductDetailCacheService.class),
                mock(ProductHotDetailCacheService.class));
    }
}
