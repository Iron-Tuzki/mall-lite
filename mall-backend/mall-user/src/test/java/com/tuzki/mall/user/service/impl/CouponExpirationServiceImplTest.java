package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponExpirationServiceImplTest {

    @Test
    void expireUnusedCouponsDelegatesToMapperAndReturnsAffectedRows() {
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponExpirationServiceImpl service = new CouponExpirationServiceImpl(userCouponMapper);
        LocalDateTime now = LocalDateTime.of(2026, 6, 13, 18, 30);
        when(userCouponMapper.expireUnusedCoupons(now)).thenReturn(3);

        int affectedRows = service.expireUnusedCoupons(now);

        assertEquals(3, affectedRows);
        verify(userCouponMapper).expireUnusedCoupons(now);
    }
}
