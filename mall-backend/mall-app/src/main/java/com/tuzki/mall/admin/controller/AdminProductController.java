package com.tuzki.mall.admin.controller;

import com.tuzki.mall.admin.product.dto.AdminProductRequest;
import com.tuzki.mall.admin.product.dto.AdminStatusUpdateRequest;
import com.tuzki.mall.admin.product.service.AdminProductService;
import com.tuzki.mall.admin.product.vo.AdminProductSkuVO;
import com.tuzki.mall.admin.product.vo.AdminProductVO;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.api.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台商品管理控制器，提供商品 SPU、SKU 和库存的一体化管理接口。
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public Result<PageResult<AdminProductVO>> listProducts(@RequestParam(required = false) Integer pageNo,
                                                           @RequestParam(required = false) Integer pageSize,
                                                           @RequestParam(required = false) Long categoryId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Integer status) {
        return Result.success(adminProductService.listProducts(pageNo, pageSize, categoryId, keyword, status));
    }

    @PostMapping
    public Result<AdminProductVO> createProduct(@Valid @RequestBody AdminProductRequest request) {
        return Result.success(adminProductService.createProduct(request));
    }

    @GetMapping("/{productId}")
    public Result<AdminProductVO> getProduct(@PathVariable Long productId) {
        return Result.success(adminProductService.getProduct(productId));
    }

    @PutMapping("/{productId}")
    public Result<AdminProductVO> updateProduct(@PathVariable Long productId,
                                                @Valid @RequestBody AdminProductRequest request) {
        return Result.success(adminProductService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    public Result<Void> deleteProduct(@PathVariable Long productId) {
        adminProductService.deleteProduct(productId);
        return Result.success();
    }

    @PutMapping("/{productId}/status")
    public Result<AdminProductVO> updateStatus(@PathVariable Long productId,
                                               @Valid @RequestBody AdminStatusUpdateRequest request) {
        return Result.success(adminProductService.updateStatus(productId, request.getStatus()));
    }

    @GetMapping("/skus")
    public Result<PageResult<AdminProductSkuVO>> listSelectableSkus(@RequestParam(required = false) Integer pageNo,
                                                                    @RequestParam(required = false) Integer pageSize,
                                                                    @RequestParam(required = false) String keyword) {
        return Result.success(adminProductService.listSelectableSkus(pageNo, pageSize, keyword));
    }
}
