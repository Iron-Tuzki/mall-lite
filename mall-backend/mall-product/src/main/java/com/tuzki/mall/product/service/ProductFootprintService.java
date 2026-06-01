package com.tuzki.mall.product.service;

import com.tuzki.mall.product.vo.ProductFootprintVO;

import java.util.List;

/**
 * 商品浏览足迹服务，负责记录、查询和清理登录用户最近浏览的商品。
 */
public interface ProductFootprintService {

    /**
     * 记录用户最近浏览的商品。
     *
     * @param userId 当前登录用户 ID
     * @param productId 商品 ID
     */
    void record(Long userId, Long productId);

    /**
     * 查询用户最近浏览的商品列表。
     *
     * @param userId 当前登录用户 ID
     * @param limit 查询条数，为空时返回允许的最大条数
     * @return 按最近浏览时间倒序排列的商品足迹
     */
    List<ProductFootprintVO> listRecent(Long userId, Integer limit);

    /**
     * 删除用户指定商品的浏览足迹。
     *
     * @param userId 当前登录用户 ID
     * @param productId 商品 ID
     */
    void remove(Long userId, Long productId);

    /**
     * 清空用户的全部浏览足迹。
     *
     * @param userId 当前登录用户 ID
     */
    void clear(Long userId);
}
