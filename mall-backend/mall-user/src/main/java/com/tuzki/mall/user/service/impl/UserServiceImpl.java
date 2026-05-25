package com.tuzki.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.user.dto.UserLoginRequest;
import com.tuzki.mall.user.dto.UserRegisterRequest;
import com.tuzki.mall.user.entity.User;
import com.tuzki.mall.user.mapper.UserMapper;
import com.tuzki.mall.user.service.LoginSessionService;
import com.tuzki.mall.user.service.UserService;
import com.tuzki.mall.user.vo.UserLoginVO;
import com.tuzki.mall.user.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户业务默认实现，负责编排注册、登录、登录态持久化和用户公开信息查询。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final int NORMAL_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private final UserMapper userMapper;

    private final LoginSessionService loginSessionService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper, LoginSessionService loginSessionService) {
        this.userMapper = userMapper;
        this.loginSessionService = loginSessionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(UserRegisterRequest request) {
        String username = request.getUsername().trim();
        if (existsByUsername(username)) {
            throw new BusinessException(400, "username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(trimToNull(request.getNickname()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setStatus(NORMAL_STATUS);
        userMapper.insert(user);
        return toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(UserLoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername().trim())
                .eq(User::getDeleted, NOT_DELETED));
        // 校验用户名和密码
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "username or password incorrect");
        }
        if (!Integer.valueOf(NORMAL_STATUS).equals(user.getStatus())) {
            throw new BusinessException(403, "user disabled");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setToken(loginSessionService.createSession(user.getId()));
        userLoginVO.setUser(toUserVO(user));
        return userLoginVO;
    }

    @Override
    public UserVO getCurrentUser(String token) {
        Long userId = loginSessionService.getUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "invalid login token");
        }
        return getById(userId);
    }

    @Override
    public void logout(String token) {
        Long userId = loginSessionService.getUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "invalid login token");
        }
        loginSessionService.deleteSession(token);
    }

    @Override
    public UserVO getById(Long id) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, id)
                .eq(User::getDeleted, NOT_DELETED));
        if (user == null) {
            throw new BusinessException(404, "user not found");
        }
        return toUserVO(user);
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return count != null && count > 0;
    }

    private UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setPhone(user.getPhone());
        userVO.setEmail(user.getEmail());
        userVO.setAvatarUrl(user.getAvatarUrl());
        userVO.setStatus(user.getStatus());
        return userVO;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
