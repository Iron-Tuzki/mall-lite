package com.tuzki.mall.admin.product.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台商品视图对象，用于返回商品 SPU 信息以及其 SKU 和库存明细。
 */
public class AdminProductVO {

    private Long id;

    private Long categoryId;

    private String productCode;

    private String name;

    private String subtitle;

    private String mainImageUrl;

    private String description;

    private Integer status;

    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<AdminProductSkuVO> skus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<AdminProductSkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<AdminProductSkuVO> skus) {
        this.skus = skus;
    }
}
