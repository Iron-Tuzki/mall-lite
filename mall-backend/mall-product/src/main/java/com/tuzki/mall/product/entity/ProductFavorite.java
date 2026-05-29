package com.tuzki.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tuzki.mall.common.entity.BaseEntity;

/**
 * 商品收藏实体，记录用户与商品之间的收藏关系。
 */
@TableName("pms_product_favorite")
public class ProductFavorite extends BaseEntity {

    private Long userId;

    private Long productId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
