package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.ProductFavorite;
import com.tuzki.mall.product.mapper.ProductFavoriteMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.service.ProductFavoriteService;
import com.tuzki.mall.product.vo.ProductFavoriteVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品收藏服务默认实现，使用 MySQL 收藏表作为收藏关系的权威存储。
 */
@Service
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int DEFAULT_LIMIT = 3;

    private static final int MAX_LIMIT = 20;

    private final ProductFavoriteMapper productFavoriteMapper;

    private final ProductMapper productMapper;

    public ProductFavoriteServiceImpl(ProductFavoriteMapper productFavoriteMapper, ProductMapper productMapper) {
        this.productFavoriteMapper = productFavoriteMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long productId) {
        ensureProductActive(productId);

        ProductFavorite productFavorite = new ProductFavorite();
        productFavorite.setUserId(userId);
        productFavorite.setProductId(productId);
        productFavorite.setDeleted(NOT_DELETED);
        try {
            productFavoriteMapper.insert(productFavorite);
        } catch (DuplicateKeyException exception) {
            // 同一用户重复收藏同一商品时保持幂等，唯一索引负责兜底防重。
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelFavorite(Long userId, Long productId) {
        productFavoriteMapper.cancelFavorite(userId, productId);
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        Long count = productFavoriteMapper.selectCount(new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getProductId, productId)
                .eq(ProductFavorite::getDeleted, NOT_DELETED));
        return count != null && count > 0;
    }

    @Override
    public Map<Long, Boolean> batchFavoriteStatus(Long userId, List<Long> productIds) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        if (userId == null || productIds == null || productIds.isEmpty()) {
            return result;
        }

        List<Long> distinctProductIds = productIds.stream()
                .filter(productId -> productId != null && productId > 0)
                .distinct()
                .toList();
        distinctProductIds.forEach(productId -> result.put(productId, false));
        if (distinctProductIds.isEmpty()) {
            return result;
        }

        Set<Long> favoritedProductIds = new HashSet<>(productFavoriteMapper.selectFavoritedProductIds(userId, distinctProductIds));
        result.replaceAll((productId, ignored) -> favoritedProductIds.contains(productId));
        return result;
    }

    @Override
    public List<ProductFavoriteVO> listFavorites(Long userId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return productFavoriteMapper.listFavorites(userId, safeLimit);
    }

    private void ensureProductActive(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED));
        if (product == null) {
            throw new BusinessException(404, "product not found");
        }
    }
}
