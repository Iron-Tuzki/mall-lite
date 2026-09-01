package com.tuzki.mall.admin.product.search;

/**
 * 商品搜索索引事件发送接口，负责将商品搜索索引变更事件交给外部消息通道。
 */
public interface ProductSearchIndexEventSender {

    /**
     * 发送商品搜索索引变更事件。
     *
     * @param event 商品搜索索引变更事件，包含商品 ID 和事件类型
     */
    void send(ProductSearchIndexChangedEvent event);
}
