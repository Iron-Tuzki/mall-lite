package com.tuzki.mall.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商品缓存配置属性，集中维护商品详情缓存前缀、基础过期时间和随机过期时间。
 */
@Component
@ConfigurationProperties(prefix = "mall.product.cache")
public class ProductCacheProperties {

    private String detailKeyPrefix = "mall:product:detail:";

    private long detailTtlMinutes = 30;

    private long detailRandomTtlMinutes = 5;

    private long detailNullTtlMinutes = 5;

    public String getDetailKeyPrefix() {
        return detailKeyPrefix;
    }

    public void setDetailKeyPrefix(String detailKeyPrefix) {
        this.detailKeyPrefix = detailKeyPrefix;
    }

    public long getDetailTtlMinutes() {
        return detailTtlMinutes;
    }

    public void setDetailTtlMinutes(long detailTtlMinutes) {
        this.detailTtlMinutes = detailTtlMinutes;
    }

    public long getDetailRandomTtlMinutes() {
        return detailRandomTtlMinutes;
    }

    public void setDetailRandomTtlMinutes(long detailRandomTtlMinutes) {
        this.detailRandomTtlMinutes = detailRandomTtlMinutes;
    }

    public long getDetailNullTtlMinutes() {
        return detailNullTtlMinutes;
    }

    public void setDetailNullTtlMinutes(long detailNullTtlMinutes) {
        this.detailNullTtlMinutes = detailNullTtlMinutes;
    }
}
