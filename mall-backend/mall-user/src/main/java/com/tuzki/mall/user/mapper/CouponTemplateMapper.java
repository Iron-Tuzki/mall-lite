package com.tuzki.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.user.entity.CouponTemplate;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券模板 Mapper，负责查询优惠券模板并在发券成功后更新模板领取数量。
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 增加优惠券模板的已领取数量，若模板停用、已删除或库存不足则不会更新。
     *
     * @param templateId 优惠券模板 ID
     * @return 受影响行数，1 表示更新成功，0 表示模板不可发放或库存不足
     */
    @Update("""
            UPDATE sms_coupon_template
            SET received_count = received_count + 1
            WHERE id = #{templateId}
              AND status = 1
              AND deleted = 0
              AND (total_stock IS NULL OR received_count < total_stock)
            """)
    int increaseReceivedCount(@Param("templateId") Long templateId);
}
