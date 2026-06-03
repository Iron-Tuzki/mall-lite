package com.tuzki.mall.product.scheduling;

import com.tuzki.mall.product.service.ProductHotService;
import com.tuzki.mall.scheduling.lock.RedisDistributedLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品热点聚合任务，定期聚合最近 24 小时热点桶生成首页热门商品榜单。
 */
@Component
public class ProductHotAggregationTask {

    private static final String LOCK_KEY = "mall:product:hot-aggregation";

    private final ProductHotService productHotService;

    public ProductHotAggregationTask(ProductHotService productHotService) {
        this.productHotService = productHotService;
    }

    /**
     * 聚合首页热门商品榜单，多实例部署时通过 Redis 分布式锁避免重复执行。
     */
    @Scheduled(
            initialDelayString = "${mall.product.hot.aggregation-fixed-delay-ms:300000}",
            fixedDelayString = "${mall.product.hot.aggregation-fixed-delay-ms:300000}"
    )
    @RedisDistributedLock(LOCK_KEY)
    public void aggregateHomepageHotProducts() {
        productHotService.aggregateHomepageHotProducts();
    }
}
