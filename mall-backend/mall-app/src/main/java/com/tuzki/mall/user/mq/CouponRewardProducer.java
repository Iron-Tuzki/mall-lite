package com.tuzki.mall.user.mq;

import com.tuzki.mall.config.rabbit.CouponRewardRabbitProperties;
import com.tuzki.mall.user.message.CouponRewardMessage;
import com.tuzki.mall.user.message.CouponRewardMessageSender;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 优惠券奖励消息生产者，负责将签到奖励消息投递到 RabbitMQ。
 *
 * @author lanshu
 * @date 2026-05-31
 */
@Component
public class CouponRewardProducer implements CouponRewardMessageSender {
    private final RabbitTemplate rabbitTemplate;

    private final CouponRewardRabbitProperties properties;

    public CouponRewardProducer(RabbitTemplate rabbitTemplate, CouponRewardRabbitProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void send(CouponRewardMessage message) {
        rabbitTemplate.convertAndSend(
                properties.getCouponRewardExchange(),
                properties.getCouponRewardRoutingKey(),
                message
        );
    }
}
