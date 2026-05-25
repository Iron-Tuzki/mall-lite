package com.tuzki.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.vo.OrderMainVO;
import jakarta.validation.Valid;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表数据访问接口，负责订单主记录的新增、查询以及基于订单状态的原子更新。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 将待支付订单原子更新为已支付。
     *
     * @param orderId 订单 ID，用于定位待支付订单
     * @param payTime 支付成功时间，用于写入订单支付时间
     * @return 影响行数，返回 1 表示更新成功，返回 0 表示订单不存在、已删除或状态不是待支付
     */
    @Update("""
            UPDATE oms_order
            SET status = 20,
                pay_time = #{payTime},
                update_time = NOW()
            WHERE id = #{orderId}
              AND status = 10
              AND deleted = 0
            """)
    int markPaidIfPending(@Param("orderId") Long orderId, @Param("payTime") LocalDateTime payTime);


    /**
     * 将待支付订单原子更新为取消。
     *
     * @param orderId 订单 ID，用于定位待支付订单
     * @return 影响行数，返回 1 表示更新成功
     */
    @Update("""
            UPDATE oms_order
            SET status = 30,
                cancel_time = NOW(),
                update_time = NOW()
            WHERE id = #{orderId}
              AND status = 10
              AND deleted = 0
            """)
    int markCancelIfPending(@Param("orderId") Long orderId);

    @Select("""
                select id as orderId,
                             order_no,
                             request_id,
                             user_id,
                             total_amount,
                             pay_amount,
                             freight_amount,
                             status,
                             create_time
                      from oms_order
                      where user_id = #{userId}
                        and deleted = 0
                      order by create_time desc
            """)
    List<OrderMainVO> listOrders(@Valid @Param("userId") Long userId);
}
