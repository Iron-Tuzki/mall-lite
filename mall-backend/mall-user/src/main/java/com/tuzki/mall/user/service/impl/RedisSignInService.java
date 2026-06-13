package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.message.CouponRewardMessage;
import com.tuzki.mall.user.message.CouponRewardMessageSender;
import com.tuzki.mall.user.service.SignInService;
import com.tuzki.mall.user.vo.SignInMonthProfileVO;
import com.tuzki.mall.user.vo.SignInProfileVO;
import com.tuzki.mall.user.vo.SignInYearlyProfileVO;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Redisson Bitmap 的用户签到服务实现，用一个 Redis Bitmap 记录用户单月每日签到状态。
 */
@Service
public class RedisSignInService implements SignInService {

    private static final String SIGN_IN_KEY_PREFIX = "mall:user:sign:";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final RedissonClient redissonClient;

    private final CouponRewardMessageSender couponRewardMessageSender;


    private static final Map<Integer, Long> SIGN_IN_REWARD_TEMPLATES = new LinkedHashMap<>();

    static {
        SIGN_IN_REWARD_TEMPLATES.put(7, 700001L);
        SIGN_IN_REWARD_TEMPLATES.put(15, 700002L);
    }

    public RedisSignInService(RedissonClient redissonClient, CouponRewardMessageSender couponRewardMessageSender) {
        this.redissonClient = redissonClient;
        this.couponRewardMessageSender = couponRewardMessageSender;
    }

    @Override
    public SignInProfileVO signInToday(Long userId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        RBitSet bitSet = getCurrentMonthBitSet(userId, today);
        boolean alreadySigned = bitSet.get(today.getDayOfMonth() - 1L);
        // Bitmap 写 1 天然幂等，重复签到不会改变已有记录；后续接积分时可使用返回旧值判断是否首次签到。
        bitSet.set(today.getDayOfMonth() - 1L, true);
        bitSet.expire(Duration.ofDays(400));
        SignInProfileVO profile = buildProfile(bitSet, today);
        if (!alreadySigned) {
            // sugus:当连续签到日期大于某个数值，则使用消息队列发放优惠券
            SIGN_IN_REWARD_TEMPLATES.forEach((requiredDays, templateId) -> {
                if (profile.getContinuousSignedDays() >= requiredDays) {
                    couponRewardMessageSender.send(buildCouponRewardMessage(userId, profile, today, requiredDays, templateId));
                }
            });
        }
        return profile;
    }

    @Override
    public SignInProfileVO getCurrentMonthProfile(Long userId) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        return buildProfile(getCurrentMonthBitSet(userId, today), today);
    }

    @Override
    public SignInYearlyProfileVO getYearlyProfile(Long userId, Integer year) {
        int targetYear = resolveTargetYear(year);
        List<SignInMonthProfileVO> months = new ArrayList<>();
        int yearSignedCount = 0;
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(targetYear, month);
            SignInMonthProfileVO monthProfile = buildMonthProfile(userId, yearMonth);
            months.add(monthProfile);
            yearSignedCount += monthProfile.getSignedCount();
        }

        SignInYearlyProfileVO profile = new SignInYearlyProfileVO();
        profile.setYear(targetYear);
        profile.setYearSignedCount(yearSignedCount);
        profile.setMonths(months);
        return profile;
    }

    private int resolveTargetYear(Integer year) {
        int currentYear = LocalDate.now(BUSINESS_ZONE).getYear();
        int targetYear = year == null ? currentYear : year;
        if (targetYear < 2000 || targetYear > currentYear + 1) {
            throw new IllegalArgumentException("sign-in year out of range");
        }
        return targetYear;
    }

    private SignInMonthProfileVO buildMonthProfile(Long userId, YearMonth yearMonth) {
        RBitSet bitSet = redissonClient.getBitSet(buildKey(userId, yearMonth.atDay(1)));
        List<Integer> signedDays = new ArrayList<>();
        int daysInMonth = yearMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            if (bitSet.get(day - 1L)) {
                signedDays.add(day);
            }
        }

        SignInMonthProfileVO profile = new SignInMonthProfileVO();
        profile.setMonth(yearMonth.getMonthValue());
        profile.setDaysInMonth(daysInMonth);
        profile.setSignedCount(signedDays.size());
        profile.setSignedDays(signedDays);
        return profile;
    }

    private SignInProfileVO buildProfile(RBitSet bitSet, LocalDate today) {
        int daysInMonth = today.lengthOfMonth();
        List<Integer> signedDays = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            if (bitSet.get(day - 1L)) {
                signedDays.add(day);
            }
        }

        SignInProfileVO profile = new SignInProfileVO();
        profile.setTodaySigned(bitSet.get(today.getDayOfMonth() - 1L));
        profile.setMonthSignedCount((int) bitSet.cardinality());
        profile.setContinuousSignedDays(countContinuousSignedDays(bitSet, today));
        profile.setYear(today.getYear());
        profile.setMonth(today.getMonthValue());
        profile.setDaysInMonth(daysInMonth);
        profile.setSignedDays(signedDays);
        return profile;
    }

    private int countContinuousSignedDays(RBitSet bitSet, LocalDate today) {
        int startDay = today.getDayOfMonth();
        if (!bitSet.get(startDay - 1L)) {
            startDay--;
        }

        int count = 0;
        for (int day = startDay; day >= 1; day--) {
            if (!bitSet.get(day - 1L)) {
                break;
            }
            count++;
        }
        return count;
    }

    private RBitSet getCurrentMonthBitSet(Long userId, LocalDate today) {
        return redissonClient.getBitSet(buildKey(userId, today));
    }

    private CouponRewardMessage buildCouponRewardMessage(Long userId, SignInProfileVO profile, LocalDate today, Integer requiredDays, Long templateId) {
        CouponRewardMessage message = new CouponRewardMessage();
        message.setUserId(userId);
        message.setContinuousSignedDays(profile.getContinuousSignedDays());
        message.setRewardMonth(YearMonth.from(today));
        message.setRequiredDays(requiredDays);
        message.setTemplateId(templateId);
        return message;
    }

    private String buildKey(Long userId, LocalDate date) {
        return SIGN_IN_KEY_PREFIX + userId + ":" + MONTH_FORMATTER.format(date);
    }
}
