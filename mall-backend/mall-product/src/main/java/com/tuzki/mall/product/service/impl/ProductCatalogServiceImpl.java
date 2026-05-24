package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.service.ProductCatalogService;
import com.tuzki.mall.product.vo.CategoryVO;
import com.tuzki.mall.product.vo.ProductDetailVO;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import com.tuzki.mall.product.vo.SkuVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of product catalog query logic.
 */
@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private final CategoryMapper categoryMapper;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    public ProductCatalogServiceImpl(CategoryMapper categoryMapper, ProductMapper productMapper, SkuMapper skuMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    @Override
    public List<CategoryVO> listCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, ACTIVE_STATUS)
                        .eq(Category::getDeleted, NOT_DELETED)
                        .orderByAsc(Category::getSort)
                        .orderByDesc(Category::getId))
                .stream()
                .map(this::toCategoryVO)
                .toList();
    }

    @Override
    public List<ProductSummaryVO> listProducts(Long categoryId) {
        LambdaQueryWrapper<Product> queryWrapper = activeProductQuery()
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId);
        if (categoryId != null) {
            queryWrapper.eq(Product::getCategoryId, categoryId);
        }
        return productMapper.selectList(queryWrapper)
                .stream()
                .map(this::toProductSummaryVO)
                .toList();
    }

    @Override
    public ProductDetailVO getProductById(Long productId) {
        Product product = productMapper.selectOne(activeProductQuery()
                .eq(Product::getId, productId));
        if (product == null) {
            throw new BusinessException(404, "product not found");
        }

        ProductDetailVO productDetailVO = toProductDetailVO(product);
        productDetailVO.setSkus(skuMapper.selectList(activeSkuQuery()
                        .eq(Sku::getProductId, productId)
                        .orderByDesc(Sku::getId))
                .stream()
                .map(this::toSkuVO)
                .toList());
        return productDetailVO;
    }

    @Override
    public SkuVO getSkuById(Long skuId) {
        Sku sku = skuMapper.selectOne(activeSkuQuery()
                .eq(Sku::getId, skuId));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        return toSkuVO(sku);
    }

    private LambdaQueryWrapper<Product> activeProductQuery() {
        return new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED);
    }

    private LambdaQueryWrapper<Sku> activeSkuQuery() {
        return new LambdaQueryWrapper<Sku>()
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED);
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        categoryVO.setId(category.getId());
        categoryVO.setParentId(category.getParentId());
        categoryVO.setName(category.getName());
        categoryVO.setLevel(category.getLevel());
        categoryVO.setSort(category.getSort());
        categoryVO.setIconUrl(category.getIconUrl());
        return categoryVO;
    }

    private ProductSummaryVO toProductSummaryVO(Product product) {
        ProductSummaryVO productSummaryVO = new ProductSummaryVO();
        fillProductSummaryVO(productSummaryVO, product);
        return productSummaryVO;
    }

    private ProductDetailVO toProductDetailVO(Product product) {
        ProductDetailVO productDetailVO = new ProductDetailVO();
        fillProductSummaryVO(productDetailVO, product);
        productDetailVO.setDescription(product.getDescription());
        return productDetailVO;
    }

    private void fillProductSummaryVO(ProductSummaryVO productSummaryVO, Product product) {
        productSummaryVO.setId(product.getId());
        productSummaryVO.setCategoryId(product.getCategoryId());
        productSummaryVO.setProductCode(product.getProductCode());
        productSummaryVO.setName(product.getName());
        productSummaryVO.setSubtitle(product.getSubtitle());
        productSummaryVO.setMainImageUrl(product.getMainImageUrl());
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO skuVO = new SkuVO();
        skuVO.setId(sku.getId());
        skuVO.setProductId(sku.getProductId());
        skuVO.setSkuCode(sku.getSkuCode());
        skuVO.setSkuName(sku.getSkuName());
        skuVO.setSpecData(sku.getSpecData());
        skuVO.setPrice(sku.getPrice());
        skuVO.setOriginalPrice(sku.getOriginalPrice());
        skuVO.setMainImageUrl(sku.getMainImageUrl());
        return skuVO;
    }
}
