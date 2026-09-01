package com.tuzki.mall.admin.product.search;

/**
 * 商品搜索索引变更事件类型，用于区分同步商品文档和删除商品文档两类消费动作。
 */
public enum ProductSearchIndexChangedEventType {

    SYNC,

    DELETE
}
