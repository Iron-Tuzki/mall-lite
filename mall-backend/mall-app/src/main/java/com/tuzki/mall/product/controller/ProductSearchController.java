package com.tuzki.mall.product.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import com.tuzki.mall.product.search.service.ProductSearchService;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 商品搜索控制器，提供基于 Elasticsearch 的商品关键词搜索和筛选聚合查询接口。
 */
@RestController
@RequestMapping("/api/products/search")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public Result<ProductSearchResultVO> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) String brandName,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) String sortBy,
                                                @RequestParam(required = false) String sortOrder,
                                                @RequestParam(required = false) Integer pageNo,
                                                @RequestParam(required = false) Integer pageSize) {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(keyword);
        request.setCategoryId(categoryId);
        request.setBrandName(brandName);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setSortBy(sortBy);
        request.setSortOrder(sortOrder);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return Result.success(productSearchService.search(request));
    }
}
