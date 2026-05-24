package com.tuzki.mall.product.controller;

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
 * REST controller for querying products.
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

    @GetMapping("/{productId}")
    public Result<ProductDetailVO> getProductById(@PathVariable Long productId) {
        return Result.success(productCatalogService.getProductById(productId));
    }
}
