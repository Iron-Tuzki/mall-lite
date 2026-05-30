package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.user.entity.CouponTemplate;
import com.tuzki.mall.user.entity.UserCoupon;
import com.tuzki.mall.user.mapper.CouponTemplateMapper;
import com.tuzki.mall.user.mapper.UserCouponMapper;
import com.tuzki.mall.user.service.CouponRewardService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 优惠券奖励服务默认实现，当前用于处理连续签到 7 天和 15 天的同步发券逻辑。
 */
@Service
public class CouponRewardServiceImpl implements CouponRewardService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int VALIDITY_TYPE_FIXED_TIME = 1;

    private static final int USER_COUPON_STATUS_UNUSED = 1;

    private static final int SOURCE_TYPE_SIGN_IN = 1;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private static final Map<Integer, Long> SIGN_IN_REWARD_TEMPLATES = new LinkedHashMap<>();

    static {
        SIGN_IN_REWARD_TEMPLATES.put(7, 700001L);
        SIGN_IN_REWARD_TEMPLATES.put(15, 700002L);
    }

    private final CouponTemplateMapper couponTemplateMapper;

    private final UserCouponMapper userCouponMapper;

    public CouponRewardServiceImpl(CouponTemplateMapper couponTemplateMapper, UserCouponMapper userCouponMapper) {
        this.couponTemplateMapper = couponTemplateMapper;
        this.userCouponMapper = userCouponMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueSignInContinuousRewards(Long userId, int continuousSignedDays, YearMonth rewardMonth) {
        SIGN_IN_REWARD_TEMPLATES.forEach((requiredDays, templateId) -> {
            if (continuousSignedDays >= requiredDays) {
                issueSignInReward(userId, rewardMonth, requiredDays, templateId);
            }
        });
    }

    private void issueSignInReward(Long userId, YearMonth rewardMonth, int requiredDays, Long templateId) {
        String sourceKey = buildSourceKey(requiredDays, rewardMonth);
        if (hasReceivedReward(userId, sourceKey)) {
            return;
        }

        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (!isAvailableTemplate(template)) {
            throw new BusinessException(404, "coupon template not found");
        }

        UserCoupon userCoupon = buildUserCoupon(userId, sourceKey, template);
        try {
            userCouponMapper.insert(userCoupon);
        } catch (DuplicateKeyException exception) {
            return;
        }

        int affectedRows = couponTemplateMapper.increaseReceivedCount(templateId);
        if (affectedRows == 0) {
            throw new BusinessException(409, "coupon stock not enough");
        }
    }

    private boolean isAvailableTemplate(CouponTemplate template) {
        return template != null
                && Integer.valueOf(ACTIVE_STATUS).equals(template.getStatus())
                && Integer.valueOf(NOT_DELETED).equals(template.getDeleted());
    }

    private boolean hasReceivedReward(Long userId, String sourceKey) {
        Long count = userCouponMapper.countBySource(userId, SOURCE_TYPE_SIGN_IN, sourceKey);
        return count != null && count > 0;
    }

    private UserCoupon buildUserCoupon(Long userId, String sourceKey, CouponTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponTemplateId(template.getId());
        userCoupon.setCouponName(template.getCouponName());
        userCoupon.setCouponType(template.getCouponType());
        userCoupon.setThresholdAmount(template.getThresholdAmount());
        userCoupon.setDiscountAmount(template.getDiscountAmount());
        userCoupon.setDiscountRate(template.getDiscountRate());
        userCoupon.setValidStartTime(resolveValidStartTime(template, now));
        userCoupon.setValidEndTime(resolveValidEndTime(template, now));
        userCoupon.setStatus(USER_COUPON_STATUS_UNUSED);
        userCoupon.setSourceType(SOURCE_TYPE_SIGN_IN);
        userCoupon.setSourceKey(sourceKey);
        userCoupon.setDeleted(NOT_DELETED);
        return userCoupon;
    }

    private LocalDateTime resolveValidStartTime(CouponTemplate template, LocalDateTime now) {
        if (Integer.valueOf(VALIDITY_TYPE_FIXED_TIME).equals(template.getValidityType())) {
            return template.getValidStartTime();
        }
        return now;
    }

    private LocalDateTime resolveValidEndTime(CouponTemplate template, LocalDateTime now) {
        if (Integer.valueOf(VALIDITY_TYPE_FIXED_TIME).equals(template.getValidityType())) {
            return template.getValidEndTime();
        }
        int validDays = template.getValidDays() == null || template.getValidDays() <= 0 ? 30 : template.getValidDays();
        return now.plusDays(validDays);
    }

    private String buildSourceKey(int requiredDays, YearMonth rewardMonth) {
        return "SIGN_IN_" + requiredDays + "_" + MONTH_FORMATTER.format(rewardMonth);
    }
}
