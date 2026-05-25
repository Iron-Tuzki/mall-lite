package com.tuzki.mall.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * SKU 库存数据访问接口，负责库存记录的基础读写以及下单锁库存等库存变更操作。
 */
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 按 SKU 锁定库存。
     *
     * @param skuId SKU ID，用于定位库存记录
     * @param quantity 锁定数量，必须大于 0
     * @return 影响行数，返回 1 表示锁定成功，返回 0 表示库存不存在、已删除或可用库存不足
     */
    @Update("""
            UPDATE ims_inventory
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE sku_id = #{skuId}
              AND deleted = 0
              AND available_stock >= #{quantity}
            """)
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 按 SKU 释放锁定库存。
     *
     * @param skuId SKU ID，用于定位库存记录
     * @param quantity 释放数量，必须大于 0
     * @return 影响行数，返回 1 表示释放成功，返回 0 表示库存不存在、已删除或锁定库存不足
     */
    @Update("""
            UPDATE ims_inventory
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE sku_id = #{skuId}
              AND deleted = 0
              AND locked_stock >= #{quantity}
            """)
    int releaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    /**
     * 按 SKU 扣减已锁定库存。
     *
     * @param skuId SKU ID，用于定位库存记录
     * @param quantity 扣减数量，必须大于 0
     * @return 影响行数，返回 1 表示扣减成功，返回 0 表示库存不存在、已删除或锁定库存不足
     */
    @Update("""
            UPDATE ims_inventory
            SET locked_stock = locked_stock - #{quantity},
                version = version + 1,
                update_time = NOW()
            WHERE sku_id = #{skuId}
              AND deleted = 0
              AND locked_stock >= #{quantity}
            """)
    int deductLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
