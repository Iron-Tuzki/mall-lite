package com.tuzki.mall.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body used when creating SKU inventory.
 */
public class InventoryCreateRequest {

    @NotNull
    private Long skuId;

    @NotNull
    @Min(0)
    private Integer availableStock;

    @NotNull
    @Min(0)
    private Integer lockedStock;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getLockedStock() {
        return lockedStock;
    }

    public void setLockedStock(Integer lockedStock) {
        this.lockedStock = lockedStock;
    }
}
