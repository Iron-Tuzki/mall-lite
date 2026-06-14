package com.tuzki.mall.admin.seckill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 后台秒杀商品请求对象，用于管理员维护活动中的 SKU、秒杀价、活动库存和限购规则。
 */
public class AdminSeckillSkuRequest {

    @NotNull(message = "skuId must not be null")
    private Long skuId;

    @NotNull(message = "seckillPrice must not be null")
    @DecimalMin(value = "0.01", message = "seckillPrice must be greater than 0")
    private BigDecimal seckillPrice;

    @NotNull(message = "stockCount must not be null")
    @Min(value = 0, message = "stockCount must not be negative")
    private Integer stockCount;

    @NotNull(message = "limitQuantity must not be null")
    @Min(value = 1, message = "limitQuantity must be greater than 0")
    private Integer limitQuantity;

    @NotNull(message = "sort must not be null")
    @Min(value = 0, message = "sort must not be negative")
    private Integer sort;

    @NotNull(message = "status must not be null")
    private Integer status;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public BigDecimal getSeckillPrice() {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Integer getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(Integer limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
