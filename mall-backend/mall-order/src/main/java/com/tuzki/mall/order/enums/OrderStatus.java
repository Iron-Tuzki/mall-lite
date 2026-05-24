package com.tuzki.mall.order.enums;

import com.tuzki.mall.common.exception.BusinessException;

import java.util.Arrays;

/**
 * 订单状态枚举，集中维护订单状态值和状态流转校验规则。
 */
public enum OrderStatus {

    PENDING_PAYMENT(10, "pending payment"),
    PAID(20, "paid"),
    CANCELLED(30, "cancelled"),
    FINISHED(40, "finished");

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
}
