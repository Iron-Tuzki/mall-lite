package com.tuzki.mall.outbox.enums;

/**
 * Outbox 消息状态，描述本地消息从待发送到已发送或等待重试的生命周期。
 */
public enum OutboxMessageStatus {

    PENDING(0),
    SENT(1),
    FAILED(2);

    private final Integer code;

    OutboxMessageStatus(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
