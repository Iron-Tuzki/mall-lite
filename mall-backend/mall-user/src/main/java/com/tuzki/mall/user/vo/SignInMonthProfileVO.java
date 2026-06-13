package com.tuzki.mall.user.vo;

import java.util.List;

/**
 * 用户单月签到视图对象，用于年度签到详情页展示某个月的签到天数和每日签到记录。
 */
public class SignInMonthProfileVO {

    private int month;

    private int daysInMonth;

    private int signedCount;

    private List<Integer> signedDays;

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDaysInMonth() {
        return daysInMonth;
    }

    public void setDaysInMonth(int daysInMonth) {
        this.daysInMonth = daysInMonth;
    }

    public int getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(int signedCount) {
        this.signedCount = signedCount;
    }

    public List<Integer> getSignedDays() {
        return signedDays;
    }

    public void setSignedDays(List<Integer> signedDays) {
        this.signedDays = signedDays;
    }
}
