package com.tuzki.mall.product.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品浏览足迹视图对象，用于展示用户最近浏览商品的摘要信息和最近浏览时间。
 */
public class ProductFootprintVO {

    private Long productId;

    private String name;

    private String subtitle;

    private String mainImageUrl;

    private BigDecimal minPrice;

    private LocalDateTime browseTime;

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

    public LocalDateTime getBrowseTime() {
        return browseTime;
    }

    public void setBrowseTime(LocalDateTime browseTime) {
        this.browseTime = browseTime;
    }
}
