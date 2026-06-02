package com.tuzki.mall.cart.message;

/**
 * 购物车逐项变更消息，携带用户、SKU、数量、版本号和操作类型。
 *
 * @param userId 用户 ID
 * @param skuId SKU ID
 * @param quantity 最新数量，删除消息固定为 0
 * @param version 购物车项单调递增版本号
 * @param operation 变更操作
 */
public record CartChangeMessage(Long userId,
                                Long skuId,
                                Integer quantity,
                                Long version,
                                CartChangeOperation operation) {
}
