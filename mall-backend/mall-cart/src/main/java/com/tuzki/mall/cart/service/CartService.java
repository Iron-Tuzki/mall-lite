package com.tuzki.mall.cart.service;

import com.tuzki.mall.cart.dto.CartAddRequest;
import com.tuzki.mall.cart.dto.CartBatchDeleteRequest;
import com.tuzki.mall.cart.dto.CartQuantityUpdateRequest;
import com.tuzki.mall.cart.vo.CartItemVO;

import java.util.List;

/**
 * 购物车业务接口，提供登录用户购物车的新增、查询、修改和删除能力。
 */
public interface CartService {

    /**
     * 将 SKU 加入用户购物车，重复加购时累加数量。
     *
     * @param userId 当前登录用户 ID
     * @param request 加购请求
     */
    void add(Long userId, CartAddRequest request);

    /**
     * 查询用户购物车列表。
     *
     * @param userId 当前登录用户 ID
     * @return 购物车项列表
     */
    List<CartItemVO> list(Long userId);

    /**
     * 修改用户购物车中指定 SKU 的数量。
     *
     * @param userId 当前登录用户 ID
     * @param skuId SKU ID
     * @param request 数量修改请求
     */
    void updateQuantity(Long userId, Long skuId, CartQuantityUpdateRequest request);

    /**
     * 删除用户购物车中的指定 SKU。
     *
     * @param userId 当前登录用户 ID
     * @param skuId SKU ID
     */
    void delete(Long userId, Long skuId);

    /**
     * 批量删除用户购物车中的 SKU。
     *
     * @param userId 当前登录用户 ID
     * @param request 批量删除请求
     */
    void batchDelete(Long userId, CartBatchDeleteRequest request);
}
