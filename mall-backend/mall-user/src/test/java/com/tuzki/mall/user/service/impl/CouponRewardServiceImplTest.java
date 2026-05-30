package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.entity.CouponTemplate;
import com.tuzki.mall.user.entity.UserCoupon;
import com.tuzki.mall.user.mapper.CouponTemplateMapper;
import com.tuzki.mall.user.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponRewardServiceImplTest {

    @Test
    void issueSignInContinuousRewardsCreatesSevenDayCoupon() {
        // sugus: mock(XXX.class)：创建模拟 Mapper 对象，替代真实的数据库 DAO，不会执行 SQL、不会操作真实库
        CouponTemplateMapper couponTemplateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponRewardServiceImpl service = new CouponRewardServiceImpl(couponTemplateMapper, userCouponMapper);
        // sugus: when(A.方法()).thenReturn(结果) = 给 Mock 方法 “预设返回值”，模拟数据库查询 / 操作的结果
        when(userCouponMapper.countBySource(1L, 1, "SIGN_IN_7_202605")).thenReturn(0L);
        when(couponTemplateMapper.selectById(700001L)).thenReturn(buildSevenDayTemplate());
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);
        when(couponTemplateMapper.increaseReceivedCount(700001L)).thenReturn(1);

        // sugus： 触发真正要测试的业务逻辑：发放连续签到奖励券
        service.issueSignInContinuousRewards(1L, 7, YearMonth.of(2026, 5));
        // sugus： ArgumentCaptor：Mockito 参数捕获器
        // sugus: 核心用途：当 Mock 方法的入参是复杂对象（比如 UserCoupon），我们想知道「Service 内部构建出来的这个对象字段对不对」，就用它把实际传入方法的对象抓出来。
        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        // sugus: captor.capture()取出捕获到的真实对象，后续做字段校验。
        // sugus: 校验 userCouponMapper.insert() 确实被调用了（证明业务走到了插入用户券这一步）
        verify(userCouponMapper).insert(captor.capture());
        UserCoupon userCoupon = captor.getValue();
        assertEquals(1L, userCoupon.getUserId());
        assertEquals(700001L, userCoupon.getCouponTemplateId());
        assertEquals("SIGN_IN_7_202605", userCoupon.getSourceKey());
        assertEquals(1, userCoupon.getSourceType());
        assertEquals(1, userCoupon.getStatus());
        assertNotNull(userCoupon.getValidStartTime());
        assertNotNull(userCoupon.getValidEndTime());
        assertTrue(userCoupon.getValidEndTime().isAfter(userCoupon.getValidStartTime()));
        verify(couponTemplateMapper).increaseReceivedCount(700001L);
    }

    @Test
    void issueSignInContinuousRewardsKeepsDuplicateRewardIdempotent() {
        CouponTemplateMapper couponTemplateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponRewardServiceImpl service = new CouponRewardServiceImpl(couponTemplateMapper, userCouponMapper);
        when(userCouponMapper.countBySource(1L, 1, "SIGN_IN_7_202605")).thenReturn(0L);
        when(couponTemplateMapper.selectById(700001L)).thenReturn(buildSevenDayTemplate());
        when(userCouponMapper.insert(any(UserCoupon.class))).thenThrow(new DuplicateKeyException("duplicate"));

        service.issueSignInContinuousRewards(1L, 7, YearMonth.of(2026, 5));

        verify(userCouponMapper).insert(any(UserCoupon.class));
        verify(couponTemplateMapper, never()).increaseReceivedCount(700001L);
    }

    @Test
    void issueSignInContinuousRewardsSkipsDatabaseInsertWhenRewardAlreadyExists() {
        CouponTemplateMapper couponTemplateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponRewardServiceImpl service = new CouponRewardServiceImpl(couponTemplateMapper, userCouponMapper);
        when(userCouponMapper.countBySource(1L, 1, "SIGN_IN_7_202605")).thenReturn(1L);

        service.issueSignInContinuousRewards(1L, 8, YearMonth.of(2026, 5));

        verify(couponTemplateMapper, never()).selectById(700001L);
        verify(userCouponMapper, never()).insert(any(UserCoupon.class));
    }

    @Test
    void issueSignInContinuousRewardsRepairsMissingSevenDayRewardOnDayEight() {
        CouponTemplateMapper couponTemplateMapper = mock(CouponTemplateMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponRewardServiceImpl service = new CouponRewardServiceImpl(couponTemplateMapper, userCouponMapper);
        when(userCouponMapper.countBySource(1L, 1, "SIGN_IN_7_202605")).thenReturn(0L);
        when(couponTemplateMapper.selectById(700001L)).thenReturn(buildSevenDayTemplate());
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);
        when(couponTemplateMapper.increaseReceivedCount(700001L)).thenReturn(1);

        service.issueSignInContinuousRewards(1L, 8, YearMonth.of(2026, 5));

        verify(userCouponMapper).insert(any(UserCoupon.class));
        verify(couponTemplateMapper).increaseReceivedCount(700001L);
    }

    private CouponTemplate buildSevenDayTemplate() {
        CouponTemplate template = new CouponTemplate();
        template.setId(700001L);
        template.setCouponName("连续签到7天满100减10券");
        template.setCouponType(1);
        template.setThresholdAmount(new BigDecimal("100.00"));
        template.setDiscountAmount(new BigDecimal("10.00"));
        template.setValidityType(2);
        template.setValidDays(30);
        template.setStatus(1);
        template.setDeleted(0);
        return template;
    }
}
