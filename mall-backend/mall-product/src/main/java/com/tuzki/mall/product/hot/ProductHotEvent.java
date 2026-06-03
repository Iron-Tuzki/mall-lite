package com.tuzki.mall.product.hot;

import java.time.LocalDateTime;

/**
 * 商品热点事件，携带商品、行为、事件标识和发生时间，用于异步统计商品热度。
 *
 * @param eventId 事件唯一标识，用于消费者幂等处理
 * @param productId 商品 ID
 * @param action 热点行为类型
 * @param occurredAt 行为发生时间
 */
public record ProductHotEvent(String eventId,
                              Long productId,
                              ProductHotAction action,
                              LocalDateTime occurredAt) {
}
