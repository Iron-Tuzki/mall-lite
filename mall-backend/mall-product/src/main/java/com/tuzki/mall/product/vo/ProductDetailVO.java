package com.tuzki.mall.product.vo;

import java.util.List;

/**
 * Public product detail view object returned by product detail APIs.
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
