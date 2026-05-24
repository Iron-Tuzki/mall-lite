package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.vo.SkuVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying SKUs.
 */
@RestController
@RequestMapping("/api/skus")
public class SkuController {

    private final ProductCatalogService productCatalogService;

    public SkuController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping("/{skuId}")
    public Result<SkuVO> getSkuById(@PathVariable Long skuId) {
        return Result.success(productCatalogService.getSkuById(skuId));
    }
}
