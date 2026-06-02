package com.tuzki.mall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.cart.entity.CartItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 购物车项数据访问接口，负责查询购物车项以及按版本号写入最新状态。
 */
public interface CartItemMapper extends BaseMapper<CartItem> {

    /**
     * 仅在消息版本号更新时覆盖购物车项，防止重复消息和乱序消息污染数据。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @param quantity 最新数量
     * @param version 消息版本号
     * @param deleted 逻辑删除标记
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO oms_cart_item(user_id, sku_id, quantity, version, deleted)
            VALUES(#{userId}, #{skuId}, #{quantity}, #{version}, #{deleted})
            ON DUPLICATE KEY UPDATE
                quantity = IF(VALUES(version) > version, VALUES(quantity), quantity),
                deleted = IF(VALUES(version) > version, VALUES(deleted), deleted),
                update_time = IF(VALUES(version) > version, NOW(), update_time),
                version = GREATEST(version, VALUES(version))
            """)
    int upsertIfNewer(@Param("userId") Long userId,
                      @Param("skuId") Long skuId,
                      @Param("quantity") Integer quantity,
                      @Param("version") Long version,
                      @Param("deleted") Integer deleted);
}
