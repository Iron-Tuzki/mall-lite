package com.tuzki.mall.payment.service;

import com.tuzki.mall.payment.dto.PaymentCallbackRequest;
import com.tuzki.mall.payment.vo.PaymentPayVO;

/**
 * 支付业务接口，负责编排模拟支付发起、支付回调处理和支付结果返回。
 */
public interface PaymentService {

    /**
     * 发起指定订单的模拟支付。
     *
     * @param orderId 订单 ID，用于定位待支付订单
     * @return 待支付流水和订单状态信息
     */
    PaymentPayVO payOrder(Long orderId);

    /**
     * 处理指定支付流水的模拟第三方回调。
     *
     * @param paymentNo 支付流水号，用于定位本次回调对应的支付记录
     * @param request 支付回调请求，包含模拟支付结果
     * @return 回调处理后的支付流水和订单状态信息
     */
    PaymentPayVO handleCallback(String paymentNo, PaymentCallbackRequest request);
}
