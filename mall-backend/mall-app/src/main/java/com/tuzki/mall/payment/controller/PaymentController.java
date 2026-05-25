package com.tuzki.mall.payment.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.payment.dto.PaymentCallbackRequest;
import com.tuzki.mall.payment.service.PaymentService;
import com.tuzki.mall.payment.vo.PaymentPayVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口，提供模拟支付回调等支付流水处理能力。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{paymentNo}/callback")
    public Result<PaymentPayVO> handleCallback(@PathVariable String paymentNo,
                                               @Valid @RequestBody PaymentCallbackRequest request) {
        return Result.success(paymentService.handleCallback(paymentNo, request));
    }
}
