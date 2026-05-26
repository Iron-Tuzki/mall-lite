package com.tuzki.mall.order.message;

/**
 * 订单超时消息发送接口，用于隔离订单业务模块与具体 MQ 实现。
 */
public interface OrderTimeoutMessageSender {

    /**
     * 发送订单超时检查消息。
     *
     * @param message 订单超时检查消息，包含订单 ID、订单号、用户 ID 和创建时间
     */
    void send(OrderTimeoutMessage message);
}
