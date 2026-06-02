package com.tuzki.mall.cart.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 购物车批量删除请求，承载结算成功后需要清理的 SKU ID 列表。
 */
public class CartBatchDeleteRequest {

    @NotEmpty(message = "skuIds must not be empty")
    private List<Long> skuIds;

    public List<Long> getSkuIds() {
        return skuIds;
    }

    public void setSkuIds(List<Long> skuIds) {
        this.skuIds = skuIds;
    }
}
