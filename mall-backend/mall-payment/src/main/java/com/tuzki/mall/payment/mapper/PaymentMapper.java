package com.tuzki.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.payment.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 支付流水数据访问接口，负责支付流水记录的新增、查询以及基于状态的原子更新。
 */
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 使用当前读并加行锁查询指定支付流水。
     *
     * @param paymentNo 支付流水号，用于定位需要读取最新状态并加锁的支付流水
     * @return 未删除的支付流水记录；如果流水不存在或已删除，返回 null
     */
    @Select("""
            SELECT id,
                   payment_no,
                   order_id,
                   order_no,
                   user_id,
                   pay_channel,
                   pay_amount,
                   status,
                   pay_time,
                   callback_content,
                   create_time,
                   update_time,
                   deleted
            FROM pms_payment
            WHERE payment_no = #{paymentNo}
              AND deleted = 0
            FOR UPDATE
            """)
    Payment selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    /**
     * 将待支付流水原子更新为支付成功。
     *
     * @param paymentNo 支付流水号，用于定位待处理的支付流水
     * @param payTime 支付成功时间，用于记录第三方确认支付成功的时间点
     * @param callbackContent 回调内容，用于保存模拟第三方支付返回的关键结果
     * @return 影响行数，返回 1 表示抢占成功，返回 0 表示流水不存在、已删除或已经被其他回调处理
     */
    @Update("""
            UPDATE pms_payment
            SET status = 20,
                pay_time = #{payTime},
                callback_content = #{callbackContent},
                update_time = NOW()
            WHERE payment_no = #{paymentNo}
              AND status = 10
              AND deleted = 0
            """)
    int markSuccessIfPending(@Param("paymentNo") String paymentNo,
                             @Param("payTime") LocalDateTime payTime,
                             @Param("callbackContent") String callbackContent);

    /**
     * 将待支付流水原子更新为支付失败。
     *
     * @param paymentNo 支付流水号，用于定位待处理的支付流水
     * @param callbackContent 回调内容，用于保存模拟第三方支付返回的关键结果
     * @return 影响行数，返回 1 表示抢占成功，返回 0 表示流水不存在、已删除或已经被其他回调处理
     */
    @Update("""
            UPDATE pms_payment
            SET status = 30,
                callback_content = #{callbackContent},
                update_time = NOW()
            WHERE payment_no = #{paymentNo}
              AND status = 10
              AND deleted = 0
            """)
    int markFailedIfPending(@Param("paymentNo") String paymentNo,
                            @Param("callbackContent") String callbackContent);
}
