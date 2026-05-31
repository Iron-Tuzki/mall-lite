package com.tuzki.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.order.entity.Order;
import com.tuzki.mall.order.vo.OrderMainVO;
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
     * @param cancelType 取消类型，用于区分用户主动取消和系统超时取消
     * @param cancelReason 取消原因，用于记录订单取消的业务说明
     * @return 影响行数，返回 1 表示更新成功
     */
    @Update("""
            UPDATE oms_order
            SET status = 30,
                cancel_type = #{cancelType},
                cancel_reason = #{cancelReason},
                cancel_time = NOW(),
                update_time = NOW()
            WHERE id = #{orderId}
              AND status = 10
              AND deleted = 0
            """)
    int markCancelIfPending(@Param("orderId") Long orderId,
                            @Param("cancelType") Integer cancelType,
                            @Param("cancelReason") String cancelReason);

    /**
     * 使用数据库行锁查询指定订单。
     *
     * @param orderId 订单 ID，用于定位需要加锁的订单主记录
     * @return 未删除的订单主记录；如果订单不存在或已删除，返回 null
     */
    @Select("""
            SELECT id,
                   order_no,
                   request_id,
                   user_id,
                   total_amount,
                   pay_amount,
                   freight_amount,
                   status,
                   cancel_type,
                   cancel_reason,
                   receiver_name,
                   receiver_phone,
                   receiver_province,
                   receiver_city,
                   receiver_district,
                   receiver_detail_address,
                   remark,
                   pay_time,
                   cancel_time,
                   finish_time,
                   create_time,
                   update_time,
                   deleted
            FROM oms_order
            WHERE id = #{orderId}
              AND deleted = 0
            FOR UPDATE
            """)
    Order selectByIdForUpdate(@Param("orderId") Long orderId);

    /**
     * 根据用户、订单状态和下单时间范围查询订单列表。
     *
     * @param userId 用户 ID
     * @param status 订单状态，为 null 时查询全部状态
     * @param startTime 下单时间范围开始值，为 null 时不限制开始时间
     * @param endTime 下单时间范围结束值，为 null 时不限制结束时间
     * @return 按创建时间倒序排列的订单主信息列表
     */
    @Select("""
            <script>
            SELECT id AS orderId,
                   order_no,
                   request_id,
                   user_id,
                   total_amount,
                   pay_amount,
                   freight_amount,
                   status,
                   cancel_type,
                   cancel_reason,
                   create_time
            FROM oms_order
            WHERE user_id = #{userId}
              AND deleted = 0
            <if test="status != null">
              AND status = #{status}
            </if>
            <if test="startTime != null">
              AND create_time &gt;= #{startTime}
            </if>
            <if test="endTime != null">
              AND create_time &lt;= #{endTime}
            </if>
            ORDER BY create_time DESC
            </script>
            """)
    List<OrderMainVO> listOrders(@Param("userId") Long userId,
                                 @Param("status") Integer status,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
}
