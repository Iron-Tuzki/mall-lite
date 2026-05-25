package com.tuzki.mall.payment.service.impl;

import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.payment.service.MockThirdPartyPaymentClient;
import org.springframework.stereotype.Component;

/**
 * 模拟第三方支付客户端默认实现，通过短暂休眠模拟外部接口网络耗时。
 */
@Component
public class MockThirdPartyPaymentClientImpl implements MockThirdPartyPaymentClient {

    // 模拟支付延迟时间
    private static final long MOCK_PAYMENT_DELAY_MILLIS = 300L;

    @Override
    public void pay(Long orderId) {
        try {
            Thread.sleep(MOCK_PAYMENT_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "mock payment interrupted");
        }
    }
}
