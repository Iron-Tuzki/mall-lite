package com.tuzki.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tuzki.mall.common.entity.BaseEntity;

/**
 * Product category entity mapped to the product category table.
 */
@TableName("pms_category")
public class Category extends BaseEntity {

    private Long parentId;

    private String name;

    private Integer level;

    private Integer sort;

    private String iconUrl;

    private Integer status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
