package com.tuzki.mall.user.vo;

/**
 * 用户登录成功后的响应对象，包含登录令牌和当前用户公开信息。
 */
public class UserLoginVO {

    private String token;

    private UserVO user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
