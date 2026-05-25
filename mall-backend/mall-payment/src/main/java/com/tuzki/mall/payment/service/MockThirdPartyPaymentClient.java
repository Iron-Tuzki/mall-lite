package com.tuzki.mall.payment.service;

/**
 * 模拟第三方支付客户端，用于模拟外部支付渠道调用耗时。
 */
public interface MockThirdPartyPaymentClient {

    /**
     * 调用模拟支付渠道。
     *
     * @param orderId 订单 ID，用于标识本次模拟支付请求
     */
    void pay(Long orderId);
}
