package com.tuzki.mall.admin.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 后台商品 SKU 请求对象，用于管理员在商品表单中维护 SKU 基础信息和库存数量。
 */
public class AdminProductSkuRequest {

    private Long id;

    @NotBlank(message = "skuCode must not be blank")
    private String skuCode;

    @NotBlank(message = "skuName must not be blank")
    private String skuName;

    private String specData;

    @NotNull(message = "price must not be null")
    @DecimalMin(value = "0.00", message = "price must not be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "originalPrice must not be negative")
    private BigDecimal originalPrice;

    private String mainImageUrl;

    @NotNull(message = "status must not be null")
    private Integer status;

    @NotNull(message = "availableStock must not be null")
    @Min(value = 0, message = "availableStock must not be negative")
    private Integer availableStock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSpecData() {
        return specData;
    }

    public void setSpecData(String specData) {
        this.specData = specData;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }
}
