package com.tuzki.mall.seckill.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 秒杀下单请求，承载活动商品、幂等请求号、收货地址、购买数量和用户备注。
 */
public class SeckillOrderCreateRequest {

    @NotNull(message = "seckillSkuId must not be null")
    private Long seckillSkuId;

    @NotBlank(message = "requestId must not be blank")
    private String requestId;

    @NotNull(message = "addressId must not be null")
    private Long addressId;

    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be greater than 0")
    private Integer quantity;

    private String remark;

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
