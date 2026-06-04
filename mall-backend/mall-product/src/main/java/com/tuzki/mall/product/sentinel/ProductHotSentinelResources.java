package com.tuzki.mall.product.sentinel;

/**
 * 商品热点 Sentinel 资源名常量，统一维护热门商品相关限流、降级和熔断资源。
 */
public final class ProductHotSentinelResources {

    public static final String HOT_PRODUCT_DETAIL = "hot-product-detail";

    private ProductHotSentinelResources() {
    }
}
