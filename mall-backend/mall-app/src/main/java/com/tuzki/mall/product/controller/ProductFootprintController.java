package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.service.ProductFootprintService;
import com.tuzki.mall.product.vo.ProductFootprintVO;
import com.tuzki.mall.user.service.LoginSessionService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品浏览足迹控制器，提供登录用户足迹查询、单条删除和全部清空接口。
 */
@RestController
@RequestMapping("/api/product-footprints")
public class ProductFootprintController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ProductFootprintService productFootprintService;

    private final LoginSessionService loginSessionService;

    public ProductFootprintController(ProductFootprintService productFootprintService,
                                      LoginSessionService loginSessionService) {
        this.productFootprintService = productFootprintService;
        this.loginSessionService = loginSessionService;
    }

    @GetMapping
    public Result<List<ProductFootprintVO>> listRecent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Integer limit) {
        return Result.success(productFootprintService.listRecent(resolveCurrentUserId(authorization), limit));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long productId) {
        productFootprintService.remove(resolveCurrentUserId(authorization), productId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear(@RequestHeader(value = "Authorization", required = false) String authorization) {
        productFootprintService.clear(resolveCurrentUserId(authorization));
        return Result.success();
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
