package com.tuzki.mall.admin.product.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 后台状态更新请求对象，用于管理员切换商品、SKU 或活动的启用状态。
 */
public class AdminStatusUpdateRequest {

    @NotNull(message = "status must not be null")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
