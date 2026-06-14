package com.tuzki.mall.admin.product.service;

import com.tuzki.mall.admin.product.dto.AdminProductRequest;
import com.tuzki.mall.admin.product.vo.AdminProductSkuVO;
import com.tuzki.mall.admin.product.vo.AdminProductVO;
import com.tuzki.mall.common.api.PageResult;

/**
 * 后台商品管理接口，负责商品 SPU、SKU 和库存的一体化维护。
 */
public interface AdminProductService {

    /**
     * 分页查询后台商品列表。
     *
     * @param pageNo 当前页码，从 1 开始
     * @param pageSize 每页数量
     * @param categoryId 商品分类 ID，可为空
     * @param keyword 商品名称或商品编码关键字，可为空
     * @param status 商品状态，可为空
     * @return 后台商品分页结果
     */
    PageResult<AdminProductVO> listProducts(Integer pageNo, Integer pageSize, Long categoryId, String keyword, Integer status);

    /**
     * 查询后台商品详情。
     *
     * @param productId 商品 ID
     * @return 商品详情，包含 SKU 和库存
     */
    AdminProductVO getProduct(Long productId);

    /**
     * 创建商品及其 SKU 和库存。
     *
     * @param request 商品创建请求
     * @return 创建后的商品详情
     */
    AdminProductVO createProduct(AdminProductRequest request);

    /**
     * 更新商品及其 SKU 和库存。
     *
     * @param productId 商品 ID
     * @param request 商品更新请求
     * @return 更新后的商品详情
     */
    AdminProductVO updateProduct(Long productId, AdminProductRequest request);

    /**
     * 软删除商品及其 SKU。
     *
     * @param productId 商品 ID
     */
    void deleteProduct(Long productId);

    /**
     * 更新商品状态。
     *
     * @param productId 商品 ID
     * @param status 商品状态
     * @return 更新后的商品详情
     */
    AdminProductVO updateStatus(Long productId, Integer status);

    /**
     * 分页查询可供秒杀选择的 SKU 池。
     *
     * @param pageNo 当前页码
     * @param pageSize 每页数量
     * @param keyword 商品名称、商品编码、SKU 名称或 SKU 编码关键字
     * @return SKU 分页结果
     */
    PageResult<AdminProductSkuVO> listSelectableSkus(Integer pageNo, Integer pageSize, String keyword);
}
