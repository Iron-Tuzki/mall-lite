package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.CursorPageResult;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.service.ProductFootprintService;
import com.tuzki.mall.product.service.ProductHotService;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import com.tuzki.mall.user.service.LoginSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品前台查询控制器，提供商品列表、推荐商品和商品详情查询接口。
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final ProductCatalogService productCatalogService;

    private final ProductFootprintService productFootprintService;

    private final ProductHotService productHotService;

    private final LoginSessionService loginSessionService;

    public ProductController(ProductCatalogService productCatalogService,
                             ProductFootprintService productFootprintService,
                             ProductHotService productHotService,
                             LoginSessionService loginSessionService) {
        this.productCatalogService = productCatalogService;
        this.productFootprintService = productFootprintService;
        this.productHotService = productHotService;
        this.loginSessionService = loginSessionService;
    }

    @GetMapping
    public Result<List<ProductSummaryVO>> listProducts(@RequestParam(required = false) Long categoryId) {
        return Result.success(productCatalogService.listProducts(categoryId));
    }

    @GetMapping("/recommend")
    public Result<PageResult<ProductSummaryVO>> recommendProducts(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(productCatalogService.recommendProducts(pageNo, pageSize));
    }

    @GetMapping("/hot")
    public Result<List<ProductSummaryVO>> hotProducts(@RequestParam(required = false) Integer limit) {
        return Result.success(productHotService.listHotProducts(limit));
    }

    @GetMapping("/recommend/scroll")
    public Result<CursorPageResult<ProductSummaryVO>> scrollRecommendProducts(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer lastSort,
            @RequestParam(required = false) Long lastId) {
        return Result.success(productCatalogService.scrollRecommendProducts(pageSize, lastSort, lastId));
    }

    @GetMapping("/{productId}")
    public Result<ProductDetailVO> getProductById(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable Long productId) {
        ProductDetailVO productDetail = productCatalogService.getProductById(productId);
        recordVisitQuietly(authorization, deviceId, productId);
        return Result.success(productDetail);
    }

    private void recordVisitQuietly(String authorization, String deviceId, Long productId) {
        Long userId = resolveUserIdQuietly(authorization, productId);
        recordFootprintQuietly(userId, productId);
        recordHotViewQuietly(userId, deviceId, productId);
    }

    private Long resolveUserIdQuietly(String authorization, Long productId) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return loginSessionService.getUserId(token);
        } catch (RuntimeException exception) {
            LOGGER.warn("resolve product visit user failed, productId={}", productId, exception);
            return null;
        }
    }

    private void recordFootprintQuietly(Long userId, Long productId) {
        if (userId == null) {
            return;
        }
        try {
            productFootprintService.record(userId, productId);
        } catch (RuntimeException exception) {
            LOGGER.warn("record product footprint failed, productId={}", productId, exception);
        }
    }

    private void recordHotViewQuietly(Long userId, String deviceId, Long productId) {
        try {
            if (userId != null) {
                productHotService.recordViewByUser(userId, productId);
            } else {
                productHotService.recordViewByDevice(deviceId, productId);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("record product hot view failed, productId={}", productId, exception);
        }
    }
}
