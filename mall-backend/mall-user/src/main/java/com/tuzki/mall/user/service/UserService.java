package com.tuzki.mall.user.service;

import com.tuzki.mall.user.dto.UserLoginRequest;
import com.tuzki.mall.user.dto.UserRegisterRequest;
import com.tuzki.mall.user.vo.UserLoginVO;
import com.tuzki.mall.user.vo.UserVO;

/**
 * 用户业务服务，负责注册、登录、登录态查询和用户公开信息查询。
 */
public interface UserService {

    /**
     * 注册新用户账号。
     *
     * @param request 用户注册请求，包含用户名、密码、昵称、手机号和邮箱
     * @return 新用户的公开信息
     */
    UserVO register(UserRegisterRequest request);

    /**
     * 用户登录并创建登录态。
     *
     * @param request 用户登录请求，包含用户名和密码
     * @return 登录令牌和用户公开信息
     */
    UserLoginVO login(UserLoginRequest request);

    /**
     * 根据登录令牌获取当前用户公开信息。
     *
     * @param token 登录令牌
     * @return 当前用户公开信息
     */
    UserVO getCurrentUser(String token);

    /**
     * 删除登录令牌，使当前登录态失效。
     *
     * @param token 登录令牌
     */
    void logout(String token);

    /**
     * 根据用户 ID 获取公开用户信息。
     *
     * @param id 用户 ID
     * @return 用户公开信息
     */
    UserVO getById(Long id);
}
