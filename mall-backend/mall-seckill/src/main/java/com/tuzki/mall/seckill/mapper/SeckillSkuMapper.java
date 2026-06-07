package com.tuzki.mall.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.seckill.entity.SeckillSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀活动商品 Mapper，负责秒杀活动商品表的基础数据访问。
 */
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {

    /**
     * 原子扣减秒杀活动商品库存。
     *
     * @param seckillSkuId 秒杀活动商品 ID
     * @param quantity 需要扣减的购买数量
     * @return 受影响行数，返回 1 表示扣减成功，返回 0 表示库存不足或活动商品不可用
     */
    @Update("""
            UPDATE sms_seckill_sku
               SET stock_count = stock_count - #{quantity},
                   update_time = NOW()
             WHERE id = #{seckillSkuId}
               AND status = 1
               AND deleted = 0
               AND stock_count >= #{quantity}
            """)
    int deductStock(@Param("seckillSkuId") Long seckillSkuId, @Param("quantity") Integer quantity);
}
