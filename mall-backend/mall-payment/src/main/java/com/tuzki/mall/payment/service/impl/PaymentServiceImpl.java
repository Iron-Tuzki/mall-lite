package com.tuzki.mall.payment.service.impl;

import com.tuzki.mall.payment.service.MockThirdPartyPaymentClient;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import org.springframework.stereotype.Service;

/**
 * 支付业务默认实现，负责编排模拟第三方支付和本地支付结果落库。
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
        // 模拟外部支付渠道调用，不放在本地数据库事务中，避免第三方耗时操作长时间占用数据库连接。
        mockThirdPartyPaymentClient.pay(orderId);
        return paymentTransactionService.confirmPaymentSuccess(orderId);
    }
}
