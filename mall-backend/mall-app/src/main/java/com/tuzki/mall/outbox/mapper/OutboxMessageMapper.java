package com.tuzki.mall.outbox.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.outbox.entity.OutboxMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 本地消息数据访问接口，负责查询待补发消息并原子更新投递状态。
 */
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {

    /**
     * 查询到达重试时间的待投递消息。
     *
     * @param now 当前时间，用于筛选已经到达 next_retry_time 的消息
     * @param batchSize 单次扫描最大消息数量
     * @return 待投递消息列表
     */
    @Select("""
            SELECT id,
                   message_id,
                   aggregate_type,
                   aggregate_id,
                   exchange_name,
                   routing_key,
                   payload_type,
                   payload,
                   status,
                   retry_count,
                   next_retry_time,
                   last_error,
                   create_time,
                   update_time,
                   deleted
            FROM sys_outbox_message
            WHERE status IN (0, 2)
              AND deleted = 0
              AND next_retry_time <= #{now}
            ORDER BY id
            LIMIT #{batchSize}
            """)
    List<OutboxMessage> listDueMessages(@Param("now") LocalDateTime now, @Param("batchSize") Integer batchSize);

    /**
     * 将消息标记为已发送。
     *
     * @param messageId 消息唯一 ID
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_outbox_message
            SET status = 1,
                last_error = NULL,
                update_time = NOW()
            WHERE message_id = #{messageId}
              AND deleted = 0
            """)
    int markSent(@Param("messageId") String messageId);

    /**
     * 标记消息投递失败并推迟下一次重试时间。
     *
     * @param messageId 消息唯一 ID
     * @param nextRetryTime 下次重试时间
     * @param lastError 最近一次失败原因
     * @return 影响行数
     */
    @Update("""
            UPDATE sys_outbox_message
            SET status = 2,
                retry_count = retry_count + 1,
                next_retry_time = #{nextRetryTime},
                last_error = #{lastError},
                update_time = NOW()
            WHERE message_id = #{messageId}
              AND deleted = 0
            """)
    int markFailed(@Param("messageId") String messageId,
                   @Param("nextRetryTime") LocalDateTime nextRetryTime,
                   @Param("lastError") String lastError);
}
