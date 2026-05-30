package com.tuzki.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.user.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户优惠券 Mapper，负责写入和查询用户实际领取到的优惠券记录。
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 根据用户和奖励来源查询已发放的优惠券数量，用于避免普通签到请求频繁触发唯一索引冲突。
     *
     * @param userId 用户 ID
     * @param sourceType 来源类型
     * @param sourceKey 来源业务唯一键
     * @return 匹配的优惠券数量
     */
    @Select("""
            SELECT COUNT(*)
            FROM sms_user_coupon
            WHERE user_id = #{userId}
              AND source_type = #{sourceType}
              AND source_key = #{sourceKey}
              AND deleted = 0
            """)
    Long countBySource(@Param("userId") Long userId,
                       @Param("sourceType") Integer sourceType,
                       @Param("sourceKey") String sourceKey);
}
