package com.tuzki.mall.user.scheduling;

import com.tuzki.mall.user.service.CouponExpirationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponExpirationTaskTest {

    @Test
    void skipsExpirationWhenDisabled() {
        CouponExpirationService couponExpirationService = mock(CouponExpirationService.class);
        CouponExpirationProperties properties = new CouponExpirationProperties();
        properties.setEnabled(false);

        new CouponExpirationTask(couponExpirationService, properties).expireUnusedCoupons();

        verify(couponExpirationService, never()).expireUnusedCoupons(any(LocalDateTime.class));
    }

    @Test
    void expiresUnusedCouponsWhenEnabled() {
        CouponExpirationService couponExpirationService = mock(CouponExpirationService.class);
        CouponExpirationProperties properties = new CouponExpirationProperties();
        properties.setEnabled(true);
        when(couponExpirationService.expireUnusedCoupons(any(LocalDateTime.class))).thenReturn(2);

        new CouponExpirationTask(couponExpirationService, properties).expireUnusedCoupons();

        verify(couponExpirationService).expireUnusedCoupons(any(LocalDateTime.class));
    }
}
