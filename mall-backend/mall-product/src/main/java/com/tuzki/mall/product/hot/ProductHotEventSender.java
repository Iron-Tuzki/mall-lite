package com.tuzki.mall.product.hot;

/**
 * 商品热点事件发送接口，用于隔离商品、购物车和支付等业务模块与具体 MQ 实现。
 */
public interface ProductHotEventSender {

    /**
     * 发送商品热点事件。
     *
     * @param event 商品热点事件，包含事件 ID、商品 ID、行为类型和发生时间
     */
    void send(ProductHotEvent event);
}
