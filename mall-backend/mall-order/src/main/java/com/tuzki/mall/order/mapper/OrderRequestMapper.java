package com.tuzki.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.order.entity.OrderRequest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 订单请求幂等表数据访问接口，负责抢占下单 requestId 以及记录 requestId 对应的订单结果。
 */
public interface OrderRequestMapper extends BaseMapper<OrderRequest> {

    /**
     * 将下单请求从处理中更新为成功，并记录本次请求最终创建的订单 ID。
     *
     * @param userId 用户 ID，用于和 requestId 一起定位下单请求
     * @param requestId 下单请求幂等号，用于识别同一次下单请求
     * @param orderId 创建成功的订单 ID，用于重复请求时快速找到原订单
     * @return 影响行数，返回 1 表示更新成功，返回 0 表示请求不存在、已删除或状态不是处理中
     */
    @Update("""
            UPDATE oms_order_request
            SET status = 20,
                order_id = #{orderId},
                update_time = NOW()
            WHERE user_id = #{userId}
              AND request_id = #{requestId}
              AND status = 10
              AND deleted = 0
            """)
    int markSuccess(@Param("userId") Long userId,
                    @Param("requestId") String requestId,
                    @Param("orderId") Long orderId);
}
