package com.tuzki.mall.payment.enums;

import com.tuzki.mall.common.exception.BusinessException;

import java.util.Arrays;

/**
 * 支付状态枚举，集中维护支付流水状态值和状态解析规则。
 */
public enum PaymentStatus {

    PENDING(10, "pending"),
    SUCCESS(20, "success"),
    FAILED(30, "failed"),
    CLOSED(40, "closed");

    private final int code;

    private final String description;

    PaymentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> Integer.valueOf(status.code).equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(400, "unknown payment status"));
    }
}
