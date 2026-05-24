package com.tuzki.mall.inventory.service;

import com.tuzki.mall.inventory.dto.InventoryCreateRequest;
import com.tuzki.mall.inventory.dto.InventoryUpdateRequest;
import com.tuzki.mall.inventory.vo.InventoryVO;

/**
 * SKU 库存业务接口，提供库存后台维护和下单交易链路中的库存锁定能力。
 */
public interface InventoryService {

    /**
     * 创建 SKU 库存记录。
     *
     * @param request 库存创建请求，包含 SKU ID、可用库存、锁定库存
     * @return 创建后的库存展示信息
     */
    InventoryVO create(InventoryCreateRequest request);

    /**
     * 根据 SKU ID 查询有效库存。
     *
     * @param skuId SKU ID
     * @return 库存展示信息
     */
    InventoryVO getBySkuId(Long skuId);

    /**
     * 根据 SKU ID 修改有效库存。
     *
     * @param skuId SKU ID
     * @param request 库存修改请求，包含可用库存和锁定库存
     * @return 修改后的库存展示信息
     */
    InventoryVO update(Long skuId, InventoryUpdateRequest request);

    /**
     * 根据 SKU ID 逻辑删除有效库存。
     *
     * @param skuId SKU ID
     */
    void delete(Long skuId);

    /**
     * 下单时锁定指定 SKU 的库存。
     *
     * @param skuId SKU ID，用于定位要锁定的库存记录
     * @param quantity 锁定数量，必须大于 0
     */
    void lockStock(Long skuId, Integer quantity);
}
