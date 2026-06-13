package com.tuzki.mall.user.service;

import java.time.LocalDateTime;

/**
 * 用户优惠券过期服务，负责把超过有效期且尚未使用的用户优惠券标记为已过期。
 */
public interface CouponExpirationService {

    /**
     * 批量过期指定时间之前已到期的未使用优惠券。
     *
     * @param now 当前业务时间，用于判断优惠券是否已经超过有效期
     * @return 本次更新为已过期状态的优惠券数量
     */
    int expireUnusedCoupons(LocalDateTime now);
}
