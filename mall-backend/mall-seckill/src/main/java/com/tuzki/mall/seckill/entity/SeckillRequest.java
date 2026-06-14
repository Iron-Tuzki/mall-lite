package com.tuzki.mall.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 秒杀请求流水实体，记录用户秒杀请求从接收、Redis 预扣、订单创建到失败补偿的状态流转。
 */
@TableName("sms_seckill_request")
public class SeckillRequest {

    public static final int STATUS_INIT = 10;

    public static final int STATUS_PRE_DEDUCTED = 20;

    public static final int STATUS_ORDER_CREATED = 30;

    public static final int STATUS_FAILED = 40;

    public static final int STATUS_COMPENSATED = 50;

    public static final int STATUS_CANCEL_COMPENSATED = 60;

    private Long id;

    private String requestId;

    private Long userId;

    private Long activityId;

    private Long seckillSkuId;

    private Long skuId;

    private Integer quantity;

    private Integer status;

    private Long orderId;

    private String failReason;

    private Integer retryCount;

    private String requestIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getSeckillSkuId() {
        return seckillSkuId;
    }

    public void setSeckillSkuId(Long seckillSkuId) {
        this.seckillSkuId = seckillSkuId;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
