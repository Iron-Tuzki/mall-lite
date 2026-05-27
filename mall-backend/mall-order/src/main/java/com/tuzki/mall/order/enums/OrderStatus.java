package com.tuzki.mall.order.enums;

import com.tuzki.mall.common.exception.BusinessException;

import java.util.Arrays;

/**
 * 订单状态枚举，集中维护订单状态值和状态流转校验规则。
 */
public enum OrderStatus {

    PENDING_PAYMENT(10, "待支付"),
    PAID(20, "已支付"),
    CANCELLED(30, "取消"),
    FINISHED(40, "完成");

    private final int code;

    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static OrderStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> Integer.valueOf(status.code).equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "unknown order status"));
    }

    public void checkCanCancel() {
        if (this == PENDING_PAYMENT) {
            return;
        }
        if (this == CANCELLED) {
            throw new BusinessException(400, "cancelled order cannot be cancelled");
        }
        if (this == PAID) {
            throw new BusinessException(400, "paid order cannot be cancelled");
        }
        if (this == FINISHED) {
            throw new BusinessException(400, "finished order cannot be cancelled");
        }
        throw new BusinessException(400, "order cannot be cancelled");
    }

    public void checkCanPay() {
        if (this == PENDING_PAYMENT) {
            return;
        }
        if (this == PAID) {
            throw new BusinessException(400, "paid order cannot be paid");
        }
        if (this == CANCELLED) {
            throw new BusinessException(400, "cancelled order cannot be paid");
        }
        if (this == FINISHED) {
            throw new BusinessException(400, "finished order cannot be paid");
        }
        throw new BusinessException(400, "order cannot be paid");
    }
}
