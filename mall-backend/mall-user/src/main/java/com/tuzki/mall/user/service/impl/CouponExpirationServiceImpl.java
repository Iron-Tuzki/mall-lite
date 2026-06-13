package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.mapper.UserCouponMapper;
import com.tuzki.mall.user.service.CouponExpirationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户优惠券过期服务默认实现，通过批量更新将已超过有效期的未使用优惠券标记为已过期。
 */
@Service
public class CouponExpirationServiceImpl implements CouponExpirationService {

    private final UserCouponMapper userCouponMapper;

    public CouponExpirationServiceImpl(UserCouponMapper userCouponMapper) {
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireUnusedCoupons(LocalDateTime now) {
        return userCouponMapper.expireUnusedCoupons(now);
    }
}
