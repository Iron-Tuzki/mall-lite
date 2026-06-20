package com.tuzki.mall.user.mq;

import com.tuzki.mall.config.rabbit.CouponRewardRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.user.message.CouponRewardMessage;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 优惠券奖励消息发送器测试，验证签到奖励消息会通过 Outbox 可靠投递。
 */
class CouponRewardSenderTest {

    @Test
    void sendCreatesOutboxMessageUsingCouponRewardExchangeAndRoutingKey() {
        OutboxMessageService outboxMessageService = mock(OutboxMessageService.class);
        CouponRewardRabbitProperties properties = new CouponRewardRabbitProperties();
        CouponRewardMessage message = buildMessage();

        new CouponRewardSender(outboxMessageService, properties).send(message);

        verify(outboxMessageService).createAndPublish(
                "COUPON_REWARD",
                "1:2026-06:7:100",
                properties.getCouponRewardExchange(),
                properties.getCouponRewardRoutingKey(),
                message
        );
    }

    private CouponRewardMessage buildMessage() {
        CouponRewardMessage message = new CouponRewardMessage();
        message.setUserId(1L);
        message.setRewardMonth(YearMonth.of(2026, 6));
        message.setRequiredDays(7);
        message.setTemplateId(100L);
        message.setContinuousSignedDays(7);
        return message;
    }
}
