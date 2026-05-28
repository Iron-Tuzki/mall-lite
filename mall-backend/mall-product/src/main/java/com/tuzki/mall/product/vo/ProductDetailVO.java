package com.tuzki.mall.product.vo;

import java.util.List;

/**
 * 商品详情视图对象，用于前台商品详情页展示商品基础信息和 SKU 列表。
 */
public class ProductDetailVO extends ProductSummaryVO {

    private String description;

    private List<SkuVO> skus;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<SkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuVO> skus) {
        this.skus = skus;
    }
}
