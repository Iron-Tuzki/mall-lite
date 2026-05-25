package com.tuzki.mall.payment.dto;

import com.tuzki.mall.payment.enums.MockPaymentResult;
import jakarta.validation.constraints.NotNull;

/**
 * 模拟支付回调请求对象，用于接收第三方支付结果。
 */
public class PaymentCallbackRequest {

    @NotNull(message = "mockResult cannot be null")
    private MockPaymentResult mockResult;

    public MockPaymentResult getMockResult() {
        return mockResult;
    }

    public void setMockResult(MockPaymentResult mockResult) {
        this.mockResult = mockResult;
    }
}
