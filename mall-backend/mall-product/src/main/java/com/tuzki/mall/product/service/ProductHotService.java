package com.tuzki.mall.product.service;

import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.vo.ProductSummaryVO;

import java.util.List;

/**
 * 商品热点服务接口，负责浏览去重、热点事件处理、榜单聚合和首页热门商品查询。
 */
public interface ProductHotService {

    /**
     * 记录登录用户浏览商品行为，同一用户短时间内重复浏览同一商品只计一次。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     */
    void recordViewByUser(Long userId, Long productId);

    /**
     * 记录匿名设备浏览商品行为，同一设备短时间内重复浏览同一商品只计一次。
     *
     * @param deviceId 匿名设备标识
     * @param productId 商品 ID
     */
    void recordViewByDevice(String deviceId, Long productId);

    /**
     * 消费并累加商品热点事件。
     *
     * @param event 商品热点事件
     */
    void handleEvent(ProductHotEvent event);

    /**
     * 聚合最近一段时间的小时热点桶，生成首页热门商品榜单。
     */
    void aggregateHomepageHotProducts();

    /**
     * 查询首页热门商品列表。
     *
     * @param limit 返回数量，服务端会限制最大值
     * @return 首页热门商品摘要列表
     */
    List<ProductSummaryVO> listHotProducts(Integer limit);
}
