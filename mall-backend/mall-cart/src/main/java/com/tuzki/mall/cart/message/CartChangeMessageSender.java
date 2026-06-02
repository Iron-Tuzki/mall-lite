package com.tuzki.mall.cart.message;

/**
 * 购物车变更消息发送接口，隔离业务模块和具体 RabbitMQ 客户端。
 */
public interface CartChangeMessageSender {

    /**
     * 发送购物车逐项变更消息。
     *
     * @param message 购物车变更消息，包含用户、SKU、数量、版本号和操作类型
     */
    void send(CartChangeMessage message);
}
