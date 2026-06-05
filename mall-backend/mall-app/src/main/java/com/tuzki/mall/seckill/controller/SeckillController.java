package com.tuzki.mall.seckill.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.seckill.dto.SeckillOrderCreateRequest;
import com.tuzki.mall.seckill.service.SeckillService;
import com.tuzki.mall.seckill.vo.SeckillActivityVO;
import com.tuzki.mall.user.service.LoginSessionService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 秒杀前台接口控制器，提供活动查询和用户秒杀下单能力。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SeckillService seckillService;

    private final LoginSessionService loginSessionService;

    public SeckillController(SeckillService seckillService, LoginSessionService loginSessionService) {
        this.seckillService = seckillService;
        this.loginSessionService = loginSessionService;
    }

    @GetMapping("/activities/active")
    public Result<List<SeckillActivityVO>> listActiveActivities() {
        return Result.success(seckillService.listActiveActivities());
    }

    @PostMapping("/orders")
    public Result<OrderCreateVO> createSeckillOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SeckillOrderCreateRequest request) {
        return Result.success(seckillService.createSeckillOrder(resolveCurrentUserId(authorization), request));
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
