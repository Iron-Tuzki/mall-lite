package com.tuzki.mall.inventory.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.inventory.dto.InventoryCreateRequest;
import com.tuzki.mall.inventory.dto.InventoryUpdateRequest;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.inventory.vo.InventoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存后台管理接口，提供按 SKU 维护库存记录的增删改查能力。
 */
@RestController
@RequestMapping("/api/admin/inventories")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    public InventoryAdminController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public Result<InventoryVO> create(@Valid @RequestBody InventoryCreateRequest request) {
        return Result.success(inventoryService.create(request));
    }

    @GetMapping("/{skuId}")
    public Result<InventoryVO> getBySkuId(@PathVariable Long skuId) {
        return Result.success(inventoryService.getBySkuId(skuId));
    }

    @PutMapping("/{skuId}")
    public Result<InventoryVO> update(@PathVariable Long skuId,
                                      @Valid @RequestBody InventoryUpdateRequest request) {
        return Result.success(inventoryService.update(skuId, request));
    }

    @DeleteMapping("/{skuId}")
    public Result<Void> delete(@PathVariable Long skuId) {
        inventoryService.delete(skuId);
        return Result.success();
    }
}
