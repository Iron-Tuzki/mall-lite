package com.tuzki.mall.product.service;

import com.tuzki.mall.product.vo.CategoryVO;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import com.tuzki.mall.product.vo.SkuVO;

import java.util.List;

/**
 * Product catalog query service for categories, products, and SKUs.
 */
public interface ProductCatalogService {

    /**
     * Lists all active product categories.
     *
     * @return active category list
     */
    List<CategoryVO> listCategories();

    /**
     * Lists all active products, optionally filtered by category id.
     *
     * @param categoryId category id filter, nullable
     * @return active product summary list
     */
    List<ProductSummaryVO> listProducts(Long categoryId);

    /**
     * Gets active product detail by product id.
     *
     * @param productId product id
     * @return product detail with active SKUs
     */
    ProductDetailVO getProductById(Long productId);

    /**
     * Gets active SKU detail by SKU id.
     *
     * @param skuId SKU id
     * @return SKU public information
     */
    SkuVO getSkuById(Long skuId);
}
