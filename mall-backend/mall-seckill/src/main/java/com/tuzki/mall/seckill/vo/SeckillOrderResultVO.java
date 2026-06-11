package com.tuzki.mall.seckill.vo;

/**
 * 秒杀下单结果视图，表示异步成单请求当前处于排队中、成功或失败状态。
 */
public class SeckillOrderResultVO {

    private Long seckillSkuId;

    private String requestId;

    private String status;

    private Long orderId;

    private String failReason;

    public Long getSeckillSkuId() {
        return seckillSkuId;
    }

    public void setSeckillSkuId(Long seckillSkuId) {
        this.seckillSkuId = seckillSkuId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
