package com.tuzki.mall.seckill.message;

import java.io.Serializable;

/**
 * 秒杀下单消息，承载 Redis 预扣成功后的异步成单所需数据。
 */
public class SeckillOrderMessage implements Serializable {

    private Long requestLogId;

    private Long userId;

    private Long seckillSkuId;

    private String requestId;

    private Long addressId;

    private Integer quantity;

    private String remark;

    public Long getRequestLogId() {
        return requestLogId;
    }

    public void setRequestLogId(Long requestLogId) {
        this.requestLogId = requestLogId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
