package com.tuzki.mall.user.vo;

import java.util.List;

/**
 * 用户签到概览视图对象，用于返回个人主页所需的本月签到统计和每日签到记录。
 */
public class SignInProfileVO {

    private boolean todaySigned;

    private int monthSignedCount;

    private int continuousSignedDays;

    private int year;

    private int month;

    private int daysInMonth;

    private List<Integer> signedDays;

    public boolean isTodaySigned() {
        return todaySigned;
    }

    public void setTodaySigned(boolean todaySigned) {
        this.todaySigned = todaySigned;
    }

    public int getMonthSignedCount() {
        return monthSignedCount;
    }

    public void setMonthSignedCount(int monthSignedCount) {
        this.monthSignedCount = monthSignedCount;
    }

    public int getContinuousSignedDays() {
        return continuousSignedDays;
    }

    public void setContinuousSignedDays(int continuousSignedDays) {
        this.continuousSignedDays = continuousSignedDays;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

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

    public List<Integer> getSignedDays() {
        return signedDays;
    }

    public void setSignedDays(List<Integer> signedDays) {
        this.signedDays = signedDays;
    }
}
