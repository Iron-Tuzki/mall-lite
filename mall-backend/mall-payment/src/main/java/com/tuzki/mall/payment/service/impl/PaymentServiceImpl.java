package com.tuzki.mall.payment.service.impl;

import com.tuzki.mall.payment.dto.PaymentCallbackRequest;
import com.tuzki.mall.payment.service.MockThirdPartyPaymentClient;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import org.springframework.stereotype.Service;

/**
 * 支付业务默认实现，负责编排模拟第三方支付调用和本地支付状态落库。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final MockThirdPartyPaymentClient mockThirdPartyPaymentClient;

    private final PaymentTransactionService paymentTransactionService;

    public PaymentServiceImpl(MockThirdPartyPaymentClient mockThirdPartyPaymentClient,
                              PaymentTransactionService paymentTransactionService) {
        this.mockThirdPartyPaymentClient = mockThirdPartyPaymentClient;
        this.paymentTransactionService = paymentTransactionService;
    }

    @Override
    public PaymentPayVO payOrder(Long orderId) {
        // 模拟外部支付渠道预下单耗时，放在本地事务外，避免长时间占用数据库连接。
        mockThirdPartyPaymentClient.pay(orderId);
        // 创建待支付订单
        PaymentPayVO pendingPayment = paymentTransactionService.createPendingPayment(orderId);
        return pendingPayment;
    }

    @Override
    public PaymentPayVO handleCallback(String paymentNo, PaymentCallbackRequest request) {
        return paymentTransactionService.handleCallback(paymentNo, request.getMockResult());
    }
}
