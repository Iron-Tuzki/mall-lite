package com.tuzki.mall.payment.service;

import com.tuzki.mall.payment.vo.PaymentPayVO;

/**
 * 支付业务接口，负责发起模拟支付并返回支付结果。
 */
public interface PaymentService {

    /**
     * 支付指定订单。
     *
     * @param orderId 订单 ID，用于定位待支付订单
     * @return 支付成功后的支付流水和订单状态信息
     */
    PaymentPayVO payOrder(Long orderId);
}
