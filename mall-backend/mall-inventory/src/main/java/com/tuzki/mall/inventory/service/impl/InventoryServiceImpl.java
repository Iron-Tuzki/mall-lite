package com.tuzki.mall.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.inventory.dto.InventoryCreateRequest;
import com.tuzki.mall.inventory.dto.InventoryUpdateRequest;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.inventory.service.InventoryService;
import com.tuzki.mall.inventory.vo.InventoryVO;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SKU 库存业务默认实现，负责后台库存维护以及交易链路中的库存锁定。
 */
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final int INITIAL_VERSION = 0;

    private final InventoryMapper inventoryMapper;

    private final SkuMapper skuMapper;

    public InventoryServiceImpl(InventoryMapper inventoryMapper, SkuMapper skuMapper) {
        this.inventoryMapper = inventoryMapper;
        this.skuMapper = skuMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryVO create(InventoryCreateRequest request) {
        ensureSkuExists(request.getSkuId());
        if (inventoryExists(request.getSkuId())) {
            throw new BusinessException(400, "inventory already exists");
        }

        Inventory inventory = new Inventory();
        inventory.setSkuId(request.getSkuId());
        inventory.setAvailableStock(request.getAvailableStock());
        inventory.setLockedStock(request.getLockedStock());
        inventory.setVersion(INITIAL_VERSION);
        inventory.setDeleted(NOT_DELETED);
        inventoryMapper.insert(inventory);
        return toInventoryVO(inventory);
    }

    @Override
    public InventoryVO getBySkuId(Long skuId) {
        return toInventoryVO(getActiveInventory(skuId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryVO update(Long skuId, InventoryUpdateRequest request) {
        Inventory inventory = getActiveInventory(skuId);
        inventory.setAvailableStock(request.getAvailableStock());
        inventory.setLockedStock(request.getLockedStock());
        inventoryMapper.updateById(inventory);
        return toInventoryVO(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long skuId) {
        Inventory inventory = getActiveInventory(skuId);
        inventory.setDeleted(DELETED);
        inventoryMapper.updateById(inventory);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockStock(Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "quantity must be greater than 0");
        }

        // 使用条件更新把“校验库存”和“锁定库存”合并为一次原子操作，避免并发下先查后改导致超卖。
        int affectedRows = inventoryMapper.lockStock(skuId, quantity);
        if (affectedRows == 1) {
            return;
        }

        Inventory inventory = getActiveInventory(skuId);
        if (inventory.getAvailableStock() < quantity) {
            throw new BusinessException(400, "insufficient stock");
        }
        throw new BusinessException(400, "lock stock failed");
    }

    private void ensureSkuExists(Long skuId) {
        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
    }

    private boolean inventoryExists(Long skuId) {
        Long count = inventoryMapper.selectCount(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, skuId));
        return count != null && count > 0;
    }

    private Inventory getActiveInventory(Long skuId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, skuId)
                .eq(Inventory::getDeleted, NOT_DELETED));
        if (inventory == null) {
            throw new BusinessException(404, "inventory not found");
        }
        return inventory;
    }

    private InventoryVO toInventoryVO(Inventory inventory) {
        InventoryVO inventoryVO = new InventoryVO();
        inventoryVO.setId(inventory.getId());
        inventoryVO.setSkuId(inventory.getSkuId());
        inventoryVO.setAvailableStock(inventory.getAvailableStock());
        inventoryVO.setLockedStock(inventory.getLockedStock());
        inventoryVO.setVersion(inventory.getVersion());
        return inventoryVO;
    }
}
