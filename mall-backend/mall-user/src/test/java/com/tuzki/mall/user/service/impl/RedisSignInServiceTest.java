package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.message.CouponRewardMessageSender;
import com.tuzki.mall.user.vo.SignInMonthProfileVO;
import com.tuzki.mall.user.vo.SignInYearlyProfileVO;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisSignInServiceTest {

    @Test
    void getYearlyProfileAggregatesTwelveMonthBitmaps() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        CouponRewardMessageSender sender = mock(CouponRewardMessageSender.class);
        RedisSignInService service = new RedisSignInService(redissonClient, sender);

        for (int month = 1; month <= 12; month++) {
            RBitSet bitSet = mock(RBitSet.class);
            if (month == 1) {
                when(bitSet.get(0L)).thenReturn(true);
                when(bitSet.get(2L)).thenReturn(true);
                when(bitSet.get(30L)).thenReturn(true);
            }
            if (month == 2) {
                when(bitSet.get(28L)).thenReturn(true);
            }
            when(redissonClient.getBitSet("mall:user:sign:9:2024%02d".formatted(month))).thenReturn(bitSet);
        }

        SignInYearlyProfileVO profile = service.getYearlyProfile(9L, 2024);

        assertEquals(2024, profile.getYear());
        assertEquals(4, profile.getYearSignedCount());
        assertEquals(12, profile.getMonths().size());

        SignInMonthProfileVO january = profile.getMonths().get(0);
        assertEquals(1, january.getMonth());
        assertEquals(31, january.getDaysInMonth());
        assertEquals(3, january.getSignedCount());
        assertEquals(List.of(1, 3, 31), january.getSignedDays());

        SignInMonthProfileVO february = profile.getMonths().get(1);
        assertEquals(2, february.getMonth());
        assertEquals(29, february.getDaysInMonth());
        assertEquals(1, february.getSignedCount());
        assertEquals(List.of(29), february.getSignedDays());
    }

    @Test
    void getYearlyProfileRejectsInvalidYear() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        CouponRewardMessageSender sender = mock(CouponRewardMessageSender.class);
        RedisSignInService service = new RedisSignInService(redissonClient, sender);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getYearlyProfile(9L, 1999));

        assertEquals("sign-in year out of range", exception.getMessage());
    }
}
