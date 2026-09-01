package com.tuzki.mall.admin.product.search;

/**
 * 商品搜索索引变更事件，描述某个商品在 Elasticsearch 搜索索引中需要执行的同步动作。
 *
 * @param productId 商品 ID
 * @param eventType 商品搜索索引变更事件类型
 */
public record ProductSearchIndexChangedEvent(Long productId, ProductSearchIndexChangedEventType eventType) {
}
