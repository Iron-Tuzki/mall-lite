package com.tuzki.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tuzki.mall.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Product SKU entity mapped to the SKU table.
 */
@TableName("pms_sku")
public class Sku extends BaseEntity {

    private Long productId;

    private String skuCode;

    private String skuName;

    private String specData;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String mainImageUrl;

    private Integer status;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
}
