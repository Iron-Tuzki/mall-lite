package com.tuzki.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tuzki.mall.common.entity.BaseEntity;

/**
 * 订单请求幂等实体，用于在下单入口抢占同一用户的同一个 requestId，避免并发重复请求进入锁库存流程。
 */
@TableName("oms_order_request")
public class OrderRequest extends BaseEntity {

    private Long userId;

    private String requestId;

    private Long orderId;

    private Integer status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
