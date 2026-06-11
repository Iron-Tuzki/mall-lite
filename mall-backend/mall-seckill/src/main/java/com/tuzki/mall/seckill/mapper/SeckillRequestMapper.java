package com.tuzki.mall.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.seckill.entity.SeckillRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀请求流水 Mapper，负责秒杀请求流水的创建、幂等查询以及状态推进。
 */
public interface SeckillRequestMapper extends BaseMapper<SeckillRequest> {

    /**
     * 插入秒杀请求流水，遇到同一用户、同一活动商品、同一请求号的重复请求时忽略。
     *
     * @param request 秒杀请求流水实体
     * @return 受影响行数，返回 1 表示插入成功，返回 0 表示已存在重复请求
     */
    @Insert("""
            INSERT IGNORE INTO sms_seckill_request
            (id, request_id, user_id, activity_id, seckill_sku_id, sku_id, quantity, status, retry_count, request_ip, deleted)
            VALUES
            (#{id}, #{requestId}, #{userId}, #{activityId}, #{seckillSkuId}, #{skuId}, #{quantity}, #{status},
             #{retryCount}, #{requestIp}, #{deleted})
            """)
    int insertIgnore(SeckillRequest request);

    /**
     * 使用当前读查询指定秒杀请求流水，确保重复请求基于最新已提交状态做业务决策。
     *
     * @param userId 用户ID
     * @param seckillSkuId 秒杀活动商品ID
     * @param requestId 秒杀请求幂等号
     * @return 秒杀请求流水，不存在时返回 null
     */
    @Select("""
            SELECT id, request_id, user_id, activity_id, seckill_sku_id, sku_id, quantity, status, order_id,
                   fail_reason, retry_count, request_ip, create_time, update_time, deleted
              FROM sms_seckill_request
             WHERE user_id = #{userId}
               AND seckill_sku_id = #{seckillSkuId}
               AND request_id = #{requestId}
               AND deleted = 0
             LIMIT 1
             FOR UPDATE
            """)
    SeckillRequest selectByUniqueKeyForUpdate(@Param("userId") Long userId,
                                              @Param("seckillSkuId") Long seckillSkuId,
                                              @Param("requestId") String requestId);

    /**
     * 查询指定秒杀请求流水，用于结果查询等不需要加锁的只读场景。
     *
     * @param userId 用户ID
     * @param seckillSkuId 秒杀活动商品ID
     * @param requestId 秒杀请求幂等号
     * @return 秒杀请求流水，不存在时返回 null
     */
    @Select("""
            SELECT id, request_id, user_id, activity_id, seckill_sku_id, sku_id, quantity, status, order_id,
                   fail_reason, retry_count, request_ip, create_time, update_time, deleted
              FROM sms_seckill_request
             WHERE user_id = #{userId}
               AND seckill_sku_id = #{seckillSkuId}
               AND request_id = #{requestId}
               AND deleted = 0
             LIMIT 1
            """)
    SeckillRequest selectByUniqueKey(@Param("userId") Long userId,
                                     @Param("seckillSkuId") Long seckillSkuId,
                                     @Param("requestId") String requestId);

    /**
     * 查询超时未完成的 Redis 预扣成功流水，用于后台补偿任务批量处理。
     *
     * @param timeoutBefore 超时边界时间，更新时间早于等于该时间才会被扫描
     * @param batchSize 单批扫描数量
     * @param maxRetryCount 最大补偿重试次数，达到该次数的流水不再扫描
     * @return 待补偿的秒杀请求流水列表
     */
    @Select("""
            SELECT id, request_id, user_id, activity_id, seckill_sku_id, sku_id, quantity, status, order_id,
                   fail_reason, retry_count, request_ip, create_time, update_time, deleted
              FROM sms_seckill_request
             WHERE status = 20
               AND update_time <= #{timeoutBefore}
               AND retry_count < #{maxRetryCount}
               AND deleted = 0
             ORDER BY update_time ASC, id ASC
             LIMIT #{batchSize}
            """)
    List<SeckillRequest> listTimedOutPreDeducted(@Param("timeoutBefore") LocalDateTime timeoutBefore,
                                                 @Param("batchSize") Integer batchSize,
                                                 @Param("maxRetryCount") Integer maxRetryCount);

    /**
     * 将请求流水推进到 Redis 预扣成功状态。
     *
     * @param id 秒杀请求流水 ID
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET status = 20,
                   update_time = NOW()
             WHERE id = #{id}
               AND deleted = 0
            """)
    int markPreDeducted(@Param("id") Long id);

    /**
     * 将请求流水推进到订单创建成功状态。
     *
     * @param id 秒杀请求流水 ID
     * @param orderId 创建成功的订单 ID
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET status = 30,
                   order_id = #{orderId},
                   fail_reason = NULL,
                   update_time = NOW()
             WHERE id = #{id}
               AND deleted = 0
            """)
    int markOrderCreated(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 将请求流水标记为失败。
     *
     * @param id 秒杀请求流水 ID
     * @param failReason 失败原因
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET status = 40,
                   fail_reason = #{failReason},
                   update_time = NOW()
             WHERE id = #{id}
               AND deleted = 0
            """)
    int markFailed(@Param("id") Long id, @Param("failReason") String failReason);

    /**
     * 将请求流水标记为已补偿。
     *
     * @param id 秒杀请求流水 ID
     * @param failReason 触发补偿的失败原因
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET status = 50,
                   fail_reason = #{failReason},
                   update_time = NOW()
             WHERE id = #{id}
               AND deleted = 0
            """)
    int markCompensated(@Param("id") Long id, @Param("failReason") String failReason);

    /**
     * 将仍处于 Redis 预扣成功状态的请求流水标记为已补偿，避免并发重复推进状态。
     *
     * @param id 秒杀请求流水 ID
     * @param failReason 补偿说明
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET status = 50,
                   fail_reason = #{failReason},
                   update_time = NOW()
             WHERE id = #{id}
               AND status = 20
               AND deleted = 0
            """)
    int markCompensatedIfPreDeducted(@Param("id") Long id, @Param("failReason") String failReason);

    /**
     * 记录后台补偿失败并增加重试次数，下一轮任务会继续处理未超过重试上限的流水。
     *
     * @param id 秒杀请求流水 ID
     * @param failReason 本次补偿失败原因
     * @return 受影响行数
     */
    @Update("""
            UPDATE sms_seckill_request
               SET retry_count = retry_count + 1,
                   fail_reason = #{failReason}
             WHERE id = #{id}
               AND status = 20
               AND deleted = 0
            """)
    int increaseCompensationRetry(@Param("id") Long id, @Param("failReason") String failReason);
}
