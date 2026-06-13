package com.tuzki.mall.user.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.user.dto.UserLoginRequest;
import com.tuzki.mall.user.dto.UserRegisterRequest;
import com.tuzki.mall.user.service.LoginSessionService;
import com.tuzki.mall.user.service.SignInService;
import com.tuzki.mall.user.service.UserService;
import com.tuzki.mall.user.vo.UserLoginVO;
import com.tuzki.mall.user.vo.SignInProfileVO;
import com.tuzki.mall.user.vo.SignInYearlyProfileVO;
import com.tuzki.mall.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 用户接口控制器，提供注册、登录、当前用户查询、退出登录和公开用户信息查询能力。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    private final LoginSessionService loginSessionService;

    private final SignInService signInService;

    public UserController(UserService userService, LoginSessionService loginSessionService, SignInService signInService) {
        this.userService = userService;
        this.loginSessionService = loginSessionService;
        this.signInService = signInService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(userService.getCurrentUser(resolveToken(authorization)));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        userService.logout(resolveToken(authorization));
        return Result.success();
    }

    @PostMapping("/sign-in")
    public Result<SignInProfileVO> signInToday(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(signInService.signInToday(resolveCurrentUserId(authorization)));
    }

    @GetMapping("/sign-in/profile")
    public Result<SignInProfileVO> getSignInProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(signInService.getCurrentMonthProfile(resolveCurrentUserId(authorization)));
    }

    @GetMapping("/sign-in/yearly")
    public Result<SignInYearlyProfileVO> getSignInYearlyProfile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                @RequestParam(value = "year", required = false) Integer year) {
        return Result.success(signInService.getYearlyProfile(resolveCurrentUserId(authorization), year));
    }

    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    private Long resolveCurrentUserId(String authorization) {
        String token = resolveToken(authorization);
        Long userId = loginSessionService.getUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "invalid login token");
        }
        return userId;
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "missing login token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "missing login token");
        }
        return token;
    }
}
