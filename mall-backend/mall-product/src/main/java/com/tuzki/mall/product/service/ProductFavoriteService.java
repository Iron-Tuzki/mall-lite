package com.tuzki.mall.product.service;

import com.tuzki.mall.product.vo.ProductFavoriteVO;

import java.util.List;
import java.util.Map;

/**
 * 商品收藏服务，负责用户收藏、取消收藏、收藏状态判断和收藏列表查询。
 */
public interface ProductFavoriteService {

    /**
     * 收藏指定商品，重复收藏时保持幂等。
     *
     * @param userId 当前登录用户 ID
     * @param productId 商品 ID
     */
    void favorite(Long userId, Long productId);

    /**
     * 取消收藏指定商品，未收藏时保持幂等。
     *
     * @param userId 当前登录用户 ID
     * @param productId 商品 ID
     */
    void cancelFavorite(Long userId, Long productId);

    /**
     * 判断当前用户是否收藏指定商品。
     *
     * @param userId 当前登录用户 ID
     * @param productId 商品 ID
     * @return 已收藏返回 true，否则返回 false
     */
    boolean isFavorited(Long userId, Long productId);

    /**
     * 批量判断当前用户是否收藏指定商品列表。
     *
     * @param userId 当前登录用户 ID
     * @param productIds 商品 ID 列表
     * @return 商品 ID 与收藏状态映射
     */
    Map<Long, Boolean> batchFavoriteStatus(Long userId, List<Long> productIds);

    /**
     * 查询当前用户最近收藏的商品。
     *
     * @param userId 当前登录用户 ID
     * @param limit 查询条数
     * @return 收藏商品列表
     */
    List<ProductFavoriteVO> listFavorites(Long userId, Integer limit);
}
