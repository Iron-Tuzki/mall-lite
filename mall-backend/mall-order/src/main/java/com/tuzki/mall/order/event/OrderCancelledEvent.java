package com.tuzki.mall.order.event;

import com.tuzki.mall.order.enums.OrderCancelType;

/**
 * 订单取消成功事件，用于通知订单外部的业务模块处理取消后的附加补偿逻辑。
 *
 * @param orderId 订单ID
 * @param cancelType 订单取消类型
 */
public record OrderCancelledEvent(Long orderId, OrderCancelType cancelType) {
}
