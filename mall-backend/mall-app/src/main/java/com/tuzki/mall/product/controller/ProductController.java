package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.CursorPageResult;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
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

    @GetMapping("/recommend/scroll")
    public Result<CursorPageResult<ProductSummaryVO>> scrollRecommendProducts(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer lastSort,
            @RequestParam(required = false) Long lastId) {
        return Result.success(productCatalogService.scrollRecommendProducts(pageSize, lastSort, lastId));
    }

    @GetMapping("/{productId}")
    public Result<ProductDetailVO> getProductById(@PathVariable Long productId) {
        return Result.success(productCatalogService.getProductById(productId));
    }
}
