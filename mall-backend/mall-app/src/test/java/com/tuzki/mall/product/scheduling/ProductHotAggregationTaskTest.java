package com.tuzki.mall.product.scheduling;

import com.tuzki.mall.product.service.ProductHotService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 商品热点聚合任务测试，验证调度任务会触发首页热门榜单聚合。
 */
class ProductHotAggregationTaskTest {

    @Test
    void aggregateHomepageHotProductsDelegatesToService() {
        ProductHotService productHotService = mock(ProductHotService.class);

        new ProductHotAggregationTask(productHotService).aggregateHomepageHotProducts();

        verify(productHotService).aggregateHomepageHotProducts();
    }
}
