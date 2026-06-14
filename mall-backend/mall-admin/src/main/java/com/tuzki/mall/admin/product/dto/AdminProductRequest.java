package com.tuzki.mall.admin.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 后台商品请求对象，用于管理员创建或编辑商品 SPU 及其 SKU 明细。
 */
public class AdminProductRequest {

    @NotNull(message = "categoryId must not be null")
    private Long categoryId;

    @NotBlank(message = "productCode must not be blank")
    private String productCode;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String subtitle;

    private String mainImageUrl;

    private String description;

    @NotNull(message = "status must not be null")
    private Integer status;

    @NotNull(message = "sort must not be null")
    @Min(value = 0, message = "sort must not be negative")
    private Integer sort;

    @Valid
    @NotEmpty(message = "skus must not be empty")
    private List<AdminProductSkuRequest> skus;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<AdminProductSkuRequest> getSkus() {
        return skus;
    }

    public void setSkus(List<AdminProductSkuRequest> skus) {
        this.skus = skus;
    }
}
