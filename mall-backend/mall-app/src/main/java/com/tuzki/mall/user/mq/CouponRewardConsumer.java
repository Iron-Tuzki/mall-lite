package com.tuzki.mall.user.mq;

import com.tuzki.mall.user.message.CouponRewardMessage;
import com.tuzki.mall.user.service.CouponRewardService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券奖励消息消费者，负责消费签到奖励消息并调用业务服务完成发券。
 */
@Component
public class CouponRewardConsumer {

    private final CouponRewardService couponRewardService;

    public CouponRewardConsumer(CouponRewardService couponRewardService) {
        this.couponRewardService = couponRewardService;
    }

    /**
     * 处理订单超时检查消息。
     *
     * @param message 订单超时检查消息，包含需要检查的订单 ID
     */
    @RabbitListener(queues = "${mall.coupon.rabbit.coupon-queue}")
    public void handle(CouponRewardMessage message) {
        couponRewardService.issueSignInContinuousRewards(message.getUserId(), message.getContinuousSignedDays(), message.getRewardMonth());
    }
}
