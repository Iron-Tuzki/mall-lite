package com.tuzki.mall.user.message;

/**
 * 发送优惠券消息的发送接口，用于隔离优惠券业务模块与具体 MQ 实现。
 */
public interface CouponRewardMessageSender {


    /**
     * 发送签到优惠券奖励消息。
     *
     * @param message 优惠券奖励消息，包含用户 ID、连续签到天数和奖励所属月份
     */
    void send(CouponRewardMessage message);
}
