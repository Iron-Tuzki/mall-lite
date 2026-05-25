package com.tuzki.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.payment.entity.Payment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 支付流水数据访问接口，负责支付流水记录的新增、查询以及基于状态的原子更新。
 */
public interface PaymentMapper extends BaseMapper<Payment> {

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
