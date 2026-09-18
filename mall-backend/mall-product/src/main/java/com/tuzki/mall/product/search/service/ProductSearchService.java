package com.tuzki.mall.product.search.service;

import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;

/**
 * 商品搜索服务接口，负责基于 Elasticsearch 提供商品全文检索、条件过滤和聚合筛选能力。
 */
public interface ProductSearchService {

    /**
     * 搜索商品并返回商品列表与筛选聚合结果。
     *
     * @param request 商品搜索请求；keyword 表示搜索关键词，categoryId 表示分类 ID，brandName 表示品牌名，
     *                minPrice 和 maxPrice 表示价格范围，sortBy 和 sortOrder 表示排序字段与排序方向，
     *                pageNo 和 pageSize 表示分页参数
     * @return 商品搜索结果，包含分页商品列表、品牌聚合、分类聚合和价格区间聚合
     */
    ProductSearchResultVO search(ProductSearchRequest request);
}
