package com.tuzki.mall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 创建订单明细请求，表示用户本次下单中的一个 SKU 及其购买数量。
 */
public class OrderCreateItemRequest {

    @NotNull(message = "skuId must not be null")
    private Long skuId;

    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be greater than 0")
    private Integer quantity;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
