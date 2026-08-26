package com.tuzki.mall.admin.product.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 商品搜索索引文档构建器，负责从 MySQL 商品、分类、SKU 和库存数据组装 Elasticsearch 文档。
 */
@Component
public class ProductSearchIndexDocumentBuilder {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final String ON_SALE_STATUS = "ON_SALE";

    private static final String OFF_SALE_STATUS = "OFF_SALE";

    private final ProductMapper productMapper;

    private final CategoryMapper categoryMapper;

    private final SkuMapper skuMapper;

    private final InventoryMapper inventoryMapper;

    public ProductSearchIndexDocumentBuilder(ProductMapper productMapper,
                                             CategoryMapper categoryMapper,
                                             SkuMapper skuMapper,
                                             InventoryMapper inventoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.skuMapper = skuMapper;
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * 把关系型数据库中的多表商品数据，转换成 ES 适合搜索的扁平文档。
     * @param productId 商品id
     * @return ES 适合搜索的扁平文档。
     */
    public Optional<ProductSearchIndexDocument> build(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || isDeleted(product.getDeleted())) {
            return Optional.empty();
        }
        Category category = categoryMapper.selectById(product.getCategoryId());
        List<Sku> skus = listActiveSkus(productId);
        List<Long> skuIds = skus.stream()
                .map(Sku::getId)
                .toList();
        List<Inventory> inventories = listInventories(skuIds);

        ProductSearchIndexDocument document = new ProductSearchIndexDocument();
        document.setProductId(product.getId());
        document.setProductName(product.getName());
        document.setProductCode(product.getProductCode());
        document.setBrandName(product.getBrandName());
        document.setCategoryId(product.getCategoryId());
        document.setCategoryName(category == null ? null : category.getName());
        document.setPrice(findMinPrice(skus));
        document.setSales(0);
        document.setStock(sumStock(inventories));
        document.setMainImageUrl(product.getMainImageUrl());
        document.setStatus(isOnSale(product.getStatus()) ? ON_SALE_STATUS : OFF_SALE_STATUS);
        document.setDescription(product.getDescription());
        return Optional.of(document);
    }

    private List<Sku> listActiveSkus(Long productId) {
        return skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED)
                .orderByAsc(Sku::getPrice));
    }

    private List<Inventory> listInventories(List<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return List.of();
        }
        return inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .in(Inventory::getSkuId, skuIds)
                .eq(Inventory::getDeleted, NOT_DELETED));
    }

    private BigDecimal findMinPrice(List<Sku> skus) {
        return skus.stream()
                .map(Sku::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private int sumStock(List<Inventory> inventories) {
        return inventories.stream()
                .map(Inventory::getAvailableStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private boolean isOnSale(Integer status) {
        return status != null && status == ACTIVE_STATUS;
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted != NOT_DELETED;
    }
}
