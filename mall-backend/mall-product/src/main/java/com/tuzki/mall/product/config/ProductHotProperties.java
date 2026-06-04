package com.tuzki.mall.product.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private int aggregationFixedDelayMs = 1800000;

    private int aggregationLimit = 50;

    private int windowHours = 24;

    /**
     * 校验商品热点配置，避免非法 Redis Key、过期时间、统计窗口或聚合参数进入运行期。
     */
    @PostConstruct
    public void validate() {
        requireText(hourKeyPrefix, "热门商品小时桶 Key 前缀不能为空");
        requireText(homepageKey, "热门商品首页榜单 Key 不能为空");
        requireText(temporaryKey, "热门商品临时榜单 Key 不能为空");
        requirePositive(bucketTtlHours, "热门商品小时桶过期时间必须大于 0");
        requirePositive(homepageTtlMinutes, "热门商品首页榜单过期时间必须大于 0");
        requirePositive(viewDedupTtlMinutes, "热门商品浏览去重时间必须大于 0");
        requirePositive(aggregationFixedDelayMs, "热门商品聚合周期必须大于 0");
        requirePositive(aggregationLimit, "热门商品聚合数量必须大于 0");
        requirePositive(windowHours, "热门商品统计窗口小时数必须大于 0");
        if (bucketTtlHours < windowHours) {
            throw new IllegalArgumentException("热门商品小时桶过期时间不能小于统计窗口小时数");
        }
    }

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

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
