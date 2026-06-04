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

    private String hotDetailKeyPrefix = "mall:product:hot:detail:";

    private String hotDetailLockKeyPrefix = "mall:product:hot:detail:lock:";

    private long detailTtlMinutes = 30;

    private long detailRandomTtlMinutes = 5;

    private long detailNullTtlMinutes = 5;

    private long hotDetailTtlMinutes = 30;

    private long hotDetailRandomTtlMinutes = 5;

    private long hotDetailNullTtlMinutes = 5;

    private long hotDetailLockWaitSeconds = 1;

    private long hotDetailLockLeaseSeconds = 10;

    private int hotDetailLockRetryTimes = 3;

    private long hotDetailLockRetryIntervalMs = 50;

    public String getDetailKeyPrefix() {
        return detailKeyPrefix;
    }

    public void setDetailKeyPrefix(String detailKeyPrefix) {
        this.detailKeyPrefix = detailKeyPrefix;
    }

    public String getHotDetailKeyPrefix() {
        return hotDetailKeyPrefix;
    }

    public void setHotDetailKeyPrefix(String hotDetailKeyPrefix) {
        this.hotDetailKeyPrefix = hotDetailKeyPrefix;
    }

    public String getHotDetailLockKeyPrefix() {
        return hotDetailLockKeyPrefix;
    }

    public void setHotDetailLockKeyPrefix(String hotDetailLockKeyPrefix) {
        this.hotDetailLockKeyPrefix = hotDetailLockKeyPrefix;
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

    public long getHotDetailTtlMinutes() {
        return hotDetailTtlMinutes;
    }

    public void setHotDetailTtlMinutes(long hotDetailTtlMinutes) {
        this.hotDetailTtlMinutes = hotDetailTtlMinutes;
    }

    public long getHotDetailRandomTtlMinutes() {
        return hotDetailRandomTtlMinutes;
    }

    public void setHotDetailRandomTtlMinutes(long hotDetailRandomTtlMinutes) {
        this.hotDetailRandomTtlMinutes = hotDetailRandomTtlMinutes;
    }

    public long getHotDetailNullTtlMinutes() {
        return hotDetailNullTtlMinutes;
    }

    public void setHotDetailNullTtlMinutes(long hotDetailNullTtlMinutes) {
        this.hotDetailNullTtlMinutes = hotDetailNullTtlMinutes;
    }

    public long getHotDetailLockWaitSeconds() {
        return hotDetailLockWaitSeconds;
    }

    public void setHotDetailLockWaitSeconds(long hotDetailLockWaitSeconds) {
        this.hotDetailLockWaitSeconds = hotDetailLockWaitSeconds;
    }

    public long getHotDetailLockLeaseSeconds() {
        return hotDetailLockLeaseSeconds;
    }

    public void setHotDetailLockLeaseSeconds(long hotDetailLockLeaseSeconds) {
        this.hotDetailLockLeaseSeconds = hotDetailLockLeaseSeconds;
    }

    public int getHotDetailLockRetryTimes() {
        return hotDetailLockRetryTimes;
    }

    public void setHotDetailLockRetryTimes(int hotDetailLockRetryTimes) {
        this.hotDetailLockRetryTimes = hotDetailLockRetryTimes;
    }

    public long getHotDetailLockRetryIntervalMs() {
        return hotDetailLockRetryIntervalMs;
    }

    public void setHotDetailLockRetryIntervalMs(long hotDetailLockRetryIntervalMs) {
        this.hotDetailLockRetryIntervalMs = hotDetailLockRetryIntervalMs;
    }
}
