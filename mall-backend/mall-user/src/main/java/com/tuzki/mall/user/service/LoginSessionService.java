package com.tuzki.mall.user.service;

/**
 * 登录态服务，负责创建、读取和删除用户登录令牌。
 */
public interface LoginSessionService {

    /**
     * 为指定用户创建登录令牌。
     *
     * @param userId 用户 ID
     * @return 可返回给客户端保存的登录令牌
     */
    String createSession(Long userId);

    /**
     * 根据登录令牌读取用户 ID。
     *
     * @param token 登录令牌
     * @return 令牌有效时返回用户 ID，否则返回 null
     */
    Long getUserId(String token);

    /**
     * 删除指定登录令牌，使其立即失效。
     *
     * @param token 登录令牌
     */
    void deleteSession(String token);
}
