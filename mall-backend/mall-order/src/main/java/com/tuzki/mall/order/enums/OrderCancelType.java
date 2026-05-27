package com.tuzki.mall.order.enums;

/**
 * 订单取消类型枚举，区分用户主动取消、系统超时自动取消等取消来源。
 */
public enum OrderCancelType {

    USER_CANCEL(10, "用户主动取消"),
    TIMEOUT_CANCEL(20, "订单超时未支付自动取消");

    private final int code;

    private final String reason;

    OrderCancelType(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }
}
