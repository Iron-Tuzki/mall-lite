package com.tuzki.mall.cart.controller;

import com.tuzki.mall.cart.dto.CartAddRequest;
import com.tuzki.mall.cart.dto.CartBatchDeleteRequest;
import com.tuzki.mall.cart.dto.CartQuantityUpdateRequest;
import com.tuzki.mall.cart.service.CartService;
import com.tuzki.mall.cart.vo.CartItemVO;
import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.user.service.LoginSessionService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车接口控制器，提供登录用户购物车的新增、查询、修改和删除接口。
 */
@RestController
@RequestMapping("/api/cart/items")
public class CartController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CartService cartService;
    private final LoginSessionService loginSessionService;

    public CartController(CartService cartService, LoginSessionService loginSessionService) {
        this.cartService = cartService;
        this.loginSessionService = loginSessionService;
    }

    @PostMapping
    public Result<Void> add(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @Valid @RequestBody CartAddRequest request) {
        cartService.add(resolveCurrentUserId(authorization), request);
        return Result.success();
    }

    @GetMapping
    public Result<List<CartItemVO>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(cartService.list(resolveCurrentUserId(authorization)));
    }

    @PutMapping("/{skuId}")
    public Result<Void> updateQuantity(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long skuId,
            @Valid @RequestBody CartQuantityUpdateRequest request) {
        cartService.updateQuantity(resolveCurrentUserId(authorization), skuId, request);
        return Result.success();
    }

    @DeleteMapping("/{skuId}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long skuId) {
        cartService.delete(resolveCurrentUserId(authorization), skuId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> batchDelete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CartBatchDeleteRequest request) {
        cartService.batchDelete(resolveCurrentUserId(authorization), request);
        return Result.success();
    }

    private Long resolveCurrentUserId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(401, "missing login token");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "missing login token");
        }
        Long userId = loginSessionService.getUserId(token);
        if (userId == null) {
            throw new BusinessException(401, "invalid login token");
        }
        return userId;
    }
}
