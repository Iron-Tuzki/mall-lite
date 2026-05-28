package com.tuzki.mall.user.service;

import com.tuzki.mall.user.vo.SignInProfileVO;

/**
 * 用户签到服务，负责基于 Redis Bitmap 记录和查询用户签到数据。
 */
public interface SignInService {

    /**
     * 执行今日签到，重复签到时保持幂等并返回当前签到概览。
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户本月签到概览
     */
    SignInProfileVO signInToday(Long userId);

    /**
     * 查询当前用户个人主页所需的本月签到概览。
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户本月签到概览
     */
    SignInProfileVO getCurrentMonthProfile(Long userId);
}
