package com.tuzki.mall.user.mq;

import com.tuzki.mall.config.rabbit.CouponRewardRabbitProperties;
import com.tuzki.mall.outbox.service.OutboxMessageService;
import com.tuzki.mall.user.message.CouponRewardMessage;
import com.tuzki.mall.user.message.CouponRewardMessageSender;
import org.springframework.stereotype.Component;

/**
 * 优惠券奖励消息发送器，负责将签到奖励消息写入 Outbox 并投递到 RabbitMQ。
 *
 * @author lanshu
 * @date 2026-05-31
 */
@Component
public class CouponRewardSender implements CouponRewardMessageSender {

    private static final String AGGREGATE_TYPE = "COUPON_REWARD";

    private final OutboxMessageService outboxMessageService;

    private final CouponRewardRabbitProperties properties;

    public CouponRewardSender(OutboxMessageService outboxMessageService, CouponRewardRabbitProperties properties) {
        this.outboxMessageService = outboxMessageService;
        this.properties = properties;
    }

    @Override
    public void send(CouponRewardMessage message) {
        outboxMessageService.createAndPublish(
                AGGREGATE_TYPE,
                aggregateId(message),
                properties.getCouponRewardExchange(),
                properties.getCouponRewardRoutingKey(),
                message
        );
    }

    private String aggregateId(CouponRewardMessage message) {
        return message.getUserId()
                + ":" + message.getRewardMonth()
                + ":" + message.getRequiredDays()
                + ":" + message.getTemplateId();
    }
}
