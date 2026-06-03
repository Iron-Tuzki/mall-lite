package com.tuzki.mall.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 商品热点配置，定义 Redis Key、统计窗口、过期时间和首页榜单数量限制。
 */
@Component
@ConfigurationProperties(prefix = "mall.product.hot")
public class ProductHotProperties {

    private String hourKeyPrefix = "mall:product:hot:hour:";

    private String homepageKey = "mall:product:hot:homepage";

    private String temporaryKey = "mall:product:hot:homepage:tmp";

    private int bucketTtlHours = 48;

    private int homepageTtlMinutes = 15;

    private int viewDedupTtlMinutes = 5;

    private int aggregationFixedDelayMs = 300000;

    private int aggregationLimit = 50;

    private int windowHours = 24;

    public String getHourKeyPrefix() {
        return hourKeyPrefix;
    }

    public void setHourKeyPrefix(String hourKeyPrefix) {
        this.hourKeyPrefix = hourKeyPrefix;
    }

    public String getHomepageKey() {
        return homepageKey;
    }

    public void setHomepageKey(String homepageKey) {
        this.homepageKey = homepageKey;
    }

    public String getTemporaryKey() {
        return temporaryKey;
    }

    public void setTemporaryKey(String temporaryKey) {
        this.temporaryKey = temporaryKey;
    }

    public int getBucketTtlHours() {
        return bucketTtlHours;
    }

    public void setBucketTtlHours(int bucketTtlHours) {
        this.bucketTtlHours = bucketTtlHours;
    }

    public int getHomepageTtlMinutes() {
        return homepageTtlMinutes;
    }

    public void setHomepageTtlMinutes(int homepageTtlMinutes) {
        this.homepageTtlMinutes = homepageTtlMinutes;
    }

    public int getViewDedupTtlMinutes() {
        return viewDedupTtlMinutes;
    }

    public void setViewDedupTtlMinutes(int viewDedupTtlMinutes) {
        this.viewDedupTtlMinutes = viewDedupTtlMinutes;
    }

    public int getAggregationFixedDelayMs() {
        return aggregationFixedDelayMs;
    }

    public void setAggregationFixedDelayMs(int aggregationFixedDelayMs) {
        this.aggregationFixedDelayMs = aggregationFixedDelayMs;
    }

    public int getAggregationLimit() {
        return aggregationLimit;
    }

    public void setAggregationLimit(int aggregationLimit) {
        this.aggregationLimit = aggregationLimit;
    }

    public int getWindowHours() {
        return windowHours;
    }

    public void setWindowHours(int windowHours) {
        this.windowHours = windowHours;
    }
}
