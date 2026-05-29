package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.service.ProductFavoriteService;
import com.tuzki.mall.product.vo.ProductFavoriteVO;
import com.tuzki.mall.user.service.LoginSessionService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 商品收藏控制器，提供收藏、取消收藏、收藏状态判断和收藏列表查询接口。
 */
@RestController
@RequestMapping("/api/product-favorites")
public class ProductFavoriteController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ProductFavoriteService productFavoriteService;

    private final LoginSessionService loginSessionService;

    public ProductFavoriteController(ProductFavoriteService productFavoriteService,
                                     LoginSessionService loginSessionService) {
        this.productFavoriteService = productFavoriteService;
        this.loginSessionService = loginSessionService;
    }

    @PostMapping("/{productId}")
    public Result<Void> favorite(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable Long productId) {
        productFavoriteService.favorite(resolveCurrentUserId(authorization), productId);
        return Result.success();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> cancelFavorite(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable Long productId) {
        productFavoriteService.cancelFavorite(resolveCurrentUserId(authorization), productId);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Boolean> isFavorited(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestParam Long productId) {
        return Result.success(productFavoriteService.isFavorited(resolveCurrentUserId(authorization), productId));
    }

    @GetMapping("/status")
    public Result<Map<Long, Boolean>> batchFavoriteStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam List<Long> productIds) {
        return Result.success(productFavoriteService.batchFavoriteStatus(resolveCurrentUserId(authorization), productIds));
    }

    @GetMapping
    public Result<List<ProductFavoriteVO>> listFavorites(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Integer limit) {
        return Result.success(productFavoriteService.listFavorites(resolveCurrentUserId(authorization), limit));
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
