package com.tuzki.mall.user.scheduling;

import com.tuzki.mall.user.service.CouponExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户优惠券过期定时任务，定期把超过有效期且尚未使用的优惠券标记为已过期。
 */
@Component
public class CouponExpirationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CouponExpirationTask.class);

    private final CouponExpirationService couponExpirationService;

    private final CouponExpirationProperties properties;

    public CouponExpirationTask(CouponExpirationService couponExpirationService, CouponExpirationProperties properties) {
        this.couponExpirationService = couponExpirationService;
        this.properties = properties;
    }

    /**
     * 扫描并过期未使用优惠券。任务关闭时直接跳过，不产生数据库更新。
     */
    @Scheduled(
            initialDelayString = "${mall.coupon.expiration.fixed-delay-ms:60000}",
            fixedDelayString = "${mall.coupon.expiration.fixed-delay-ms:60000}"
    )
    public void expireUnusedCoupons() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return;
        }
        long begin = System.currentTimeMillis();
        int affectedRows = couponExpirationService.expireUnusedCoupons(LocalDateTime.now());
        LOGGER.info("用户优惠券过期任务执行完成，过期数量={}，耗时={}ms", affectedRows, System.currentTimeMillis() - begin);
    }
}
