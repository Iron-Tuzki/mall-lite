package com.tuzki.mall.payment.enums;

/**
 * 支付渠道枚举，当前第一版只提供模拟支付渠道。
 */
public enum PayChannel {

    MOCK(1, "mock payment");

    private final int code;

    private final String description;

    PayChannel(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
