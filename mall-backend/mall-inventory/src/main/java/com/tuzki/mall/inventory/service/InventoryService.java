package com.tuzki.mall.inventory.service;

import com.tuzki.mall.inventory.dto.InventoryCreateRequest;
import com.tuzki.mall.inventory.dto.InventoryUpdateRequest;
import com.tuzki.mall.inventory.vo.InventoryVO;

/**
 * Inventory business service for maintaining SKU inventory records.
 */
public interface InventoryService {

    /**
     * Creates an inventory record for a SKU.
     *
     * @param request inventory creation request, including SKU id and stock values
     * @return created inventory public information
     */
    InventoryVO create(InventoryCreateRequest request);

    /**
     * Gets active inventory by SKU id.
     *
     * @param skuId SKU id
     * @return inventory public information
     */
    InventoryVO getBySkuId(Long skuId);

    /**
     * Updates active inventory by SKU id.
     *
     * @param skuId SKU id
     * @param request inventory update request
     * @return updated inventory public information
     */
    InventoryVO update(Long skuId, InventoryUpdateRequest request);

    /**
     * Logically deletes active inventory by SKU id.
     *
     * @param skuId SKU id
     */
    void delete(Long skuId);
}
