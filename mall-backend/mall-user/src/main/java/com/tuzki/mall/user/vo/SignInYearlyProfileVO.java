package com.tuzki.mall.user.vo;

import java.util.List;

/**
 * 用户年度签到视图对象，用于年度详情页汇总全年签到天数和 12 个月签到热力图数据。
 */
public class SignInYearlyProfileVO {

    private int year;

    private int yearSignedCount;

    private List<SignInMonthProfileVO> months;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYearSignedCount() {
        return yearSignedCount;
    }

    public void setYearSignedCount(int yearSignedCount) {
        this.yearSignedCount = yearSignedCount;
    }

    public List<SignInMonthProfileVO> getMonths() {
        return months;
    }

    public void setMonths(List<SignInMonthProfileVO> months) {
        this.months = months;
    }
}
