package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.vo.CategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for querying product categories.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductCatalogService productCatalogService;

    public CategoryController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(productCatalogService.listCategories());
    }
}
