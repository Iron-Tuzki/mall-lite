package com.tuzki.mall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tuzki.mall.common.entity.BaseEntity;

/**
 * 购物车项实体，记录用户 SKU 数量、版本号和逻辑删除墓碑。
 */
@TableName("oms_cart_item")
public class CartItem extends BaseEntity {

    private Long userId;

    private Long skuId;

    private Integer quantity;

    private Long version;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
