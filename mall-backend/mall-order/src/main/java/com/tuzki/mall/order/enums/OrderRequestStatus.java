package com.tuzki.mall.order.enums;

/**
 * 订单请求处理状态枚举，用于标识下单幂等请求当前处于处理中还是已成功创建订单。
 */
public enum OrderRequestStatus {

    PROCESSING(10),

    SUCCESS(20);

    private final int code;

    OrderRequestStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
