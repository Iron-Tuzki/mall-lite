package com.tuzki.mall.cart.service;

/**
 * Redis 中保存的购物车项值，删除时保留墓碑和版本号。
 *
 * @param quantity 当前数量，墓碑固定为 0
 * @param version 单调递增版本号
 * @param deleted 是否为删除墓碑
 */
public record CartCacheItem(Integer quantity, Long version, boolean deleted) {
}
