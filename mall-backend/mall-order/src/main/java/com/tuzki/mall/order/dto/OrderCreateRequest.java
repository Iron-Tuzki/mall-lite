package com.tuzki.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 创建订单请求，承载幂等请求号、收货地址、订单明细列表和用户备注。
 */
public class OrderCreateRequest {

    @NotBlank(message = "requestId must not be blank")
    private String requestId;

    @NotNull(message = "addressId must not be null")
    private Long addressId;

    @Valid
    @NotEmpty(message = "items must not be empty")
    private List<OrderCreateItemRequest> items;

    private String remark;

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

    public List<OrderCreateItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderCreateItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
