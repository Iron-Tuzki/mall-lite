package com.tuzki.mall.product.service;

import com.tuzki.mall.common.api.CursorPageResult;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.product.vo.CategoryVO;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import com.tuzki.mall.product.vo.SkuVO;

import java.util.List;

/**
 * 商品目录查询接口，负责为前台提供分类、商品、SKU 和推荐商品等浏览能力。
 */
public interface ProductCatalogService {

    /**
     * 查询所有启用状态的商品分类。
     *
     * @return 启用状态的商品分类列表
     */
    List<CategoryVO> listCategories();

    /**
     * 查询启用状态的商品列表，可按分类过滤。
     *
     * @param categoryId 商品分类 ID，可为空；为空时查询所有启用商品
     * @return 商品摘要列表
     */
    List<ProductSummaryVO> listProducts(Long categoryId);

    /**
     * 分页查询推荐商品列表。
     *
     * @param pageNo 当前页码，从 1 开始
     * @param pageSize 每页商品数量，服务端会限制最大值
     * @return 推荐商品分页摘要
     */
    PageResult<ProductSummaryVO> recommendProducts(Integer pageNo, Integer pageSize);

    /**
     * 游标分页查询推荐商品列表，适合首页无限滚动，避免大页码 OFFSET 查询越来越慢。
     *
     * @param pageSize 每次加载的商品数量，服务端会限制最大值
     * @param lastSort 上一次返回批次中最后一个商品的排序值，首次查询可为空
     * @param lastId 上一次返回批次中最后一个商品的商品 ID，首次查询可为空
     * @return 推荐商品游标分页摘要
     */
    CursorPageResult<ProductSummaryVO> scrollRecommendProducts(Integer pageSize, Integer lastSort, Long lastId);

    /**
     * 根据商品 ID 查询启用状态的商品详情。
     *
     * @param productId 商品 ID
     * @return 商品详情和启用状态的 SKU 列表
     */
    ProductDetailVO getProductById(Long productId);

    /**
     * 根据 SKU ID 查询启用状态的 SKU 信息。
     *
     * @param skuId SKU ID
     * @return SKU 前台展示信息
     */
    SkuVO getSkuById(Long skuId);
}
