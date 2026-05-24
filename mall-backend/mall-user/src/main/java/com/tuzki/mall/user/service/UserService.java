package com.tuzki.mall.user.service;

import com.tuzki.mall.user.dto.UserRegisterRequest;
import com.tuzki.mall.user.vo.UserVO;

/**
 * User business service for account registration and user profile queries.
 */
public interface UserService {

    /**
     * Registers a new user account.
     *
     * @param request user registration request, including username, password, nickname, phone, and email
     * @return public information of the newly created user
     */
    UserVO register(UserRegisterRequest request);

    /**
     * Gets public user information by user id.
     *
     * @param id user id
     * @return public user information
     */
    UserVO getById(Long id);
}
