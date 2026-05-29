package com.tuzki.mall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品收藏视图对象，用于展示用户收藏的商品摘要信息。
 */
public class ProductFavoriteVO {

    private Long productId;

    private String name;

    private String subtitle;

    private String mainImageUrl;

    private BigDecimal minPrice;

    private LocalDateTime favoriteTime;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public LocalDateTime getFavoriteTime() {
        return favoriteTime;
    }

    public void setFavoriteTime(LocalDateTime favoriteTime) {
        this.favoriteTime = favoriteTime;
    }
}
