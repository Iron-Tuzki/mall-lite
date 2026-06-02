package com.tuzki.mall.cart.service;

/**
 * Redis 购物车项变更结果，保留旧值以便消息发送失败时执行条件回滚。
 *
 * @param previousJson 变更前 JSON，新建字段时为 null
 * @param current 变更后的购物车项
 */
public record CartCacheMutation(String previousJson, CartCacheItem current) {
}
