package com.tuzki.mall.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.payment.entity.Payment;
import com.tuzki.mall.payment.enums.PayChannel;
import com.tuzki.mall.payment.enums.PaymentStatus;
import com.tuzki.mall.payment.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 支付流水并发控制集成测试，用于验证支付状态 CAS 更新只能被一个回调请求抢占成功。
 */
@SpringBootTest
@Transactional
class PaymentMapperConcurrencyTest {

    @Autowired
    private PaymentMapper paymentMapper;

    @Test
    void onlyOneCallbackCanChangePendingPaymentToSuccess() {
        Payment payment = new Payment();
        payment.setPaymentNo("P-CAS-" + System.nanoTime());
        payment.setOrderId(1L);
        payment.setOrderNo("O-CAS-" + System.nanoTime());
        payment.setUserId(1L);
        payment.setPayChannel(PayChannel.MOCK.getCode());
        payment.setPayAmount(new BigDecimal("199.00"));
        payment.setStatus(PaymentStatus.PENDING.getCode());
        payment.setDeleted(0);
        paymentMapper.insert(payment);

        int firstAffectedRows = paymentMapper.markSuccessIfPending(
                payment.getPaymentNo(),
                LocalDateTime.now(),
                "{\"channel\":\"mock\",\"result\":\"success\"}"
        );
        int secondAffectedRows = paymentMapper.markSuccessIfPending(
                payment.getPaymentNo(),
                LocalDateTime.now(),
                "{\"channel\":\"mock\",\"result\":\"success\"}"
        );

        Payment currentPayment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getPaymentNo, payment.getPaymentNo()));
        assertEquals(1, firstAffectedRows);
        assertEquals(0, secondAffectedRows);
        assertEquals(PaymentStatus.SUCCESS.getCode(), currentPayment.getStatus());
    }
}
