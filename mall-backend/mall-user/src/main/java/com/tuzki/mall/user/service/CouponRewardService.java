package com.tuzki.mall.user.service;

import java.time.YearMonth;

/**
 * 优惠券奖励服务，负责根据用户行为发放对应的优惠券奖励。
 */
public interface CouponRewardService {

    /**
     * 根据用户连续签到天数发放签到奖励优惠券。
     *
     * @param userId 用户 ID
     * @param continuousSignedDays 当前连续签到天数
     * @param rewardMonth 奖励所属月份，用于生成月度幂等来源键
     */
    void issueSignInContinuousRewards(Long userId, int continuousSignedDays, YearMonth rewardMonth);
}
