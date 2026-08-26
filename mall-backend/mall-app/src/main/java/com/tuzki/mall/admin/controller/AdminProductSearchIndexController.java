package com.tuzki.mall.admin.controller;

import com.tuzki.mall.admin.product.search.ProductSearchIndexService;
import com.tuzki.mall.common.api.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台商品搜索索引控制器，提供商品数据同步到 Elasticsearch 的手动维护接口。
 */
@RestController
@RequestMapping("/api/admin/products/search-index")
public class AdminProductSearchIndexController {

    private final ProductSearchIndexService productSearchIndexService;

    public AdminProductSearchIndexController(ProductSearchIndexService productSearchIndexService) {
        this.productSearchIndexService = productSearchIndexService;
    }

    @PostMapping("/rebuild")
    public Result<Void> rebuildProductSearchIndex() {
        productSearchIndexService.rebuildAll();
        return Result.success();
    }

    @PostMapping("/{productId}/sync")
    public Result<Void> syncSingleProduct(@PathVariable Long productId) {
        productSearchIndexService.syncProduct(productId);
        return Result.success();
    }
}
