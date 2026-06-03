package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.ProductFavorite;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import com.tuzki.mall.product.mapper.ProductFavoriteMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.service.ProductFavoriteCacheService;
import com.tuzki.mall.product.service.ProductFavoriteService;
import com.tuzki.mall.product.vo.ProductFavoriteVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 商品收藏服务默认实现，使用 MySQL 收藏表作为收藏关系的权威存储。
 */
@Service
public class ProductFavoriteServiceImpl implements ProductFavoriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductFavoriteServiceImpl.class);

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int DEFAULT_LIMIT = 3;

    private static final int MAX_LIMIT = 20;

    private final ProductFavoriteMapper productFavoriteMapper;

    private final ProductMapper productMapper;

    private final ProductFavoriteCacheService productFavoriteCacheService;

    private final ProductHotEventSender productHotEventSender;

    public ProductFavoriteServiceImpl(ProductFavoriteMapper productFavoriteMapper,
                                      ProductMapper productMapper,
                                      ProductFavoriteCacheService productFavoriteCacheService,
                                      ProductHotEventSender productHotEventSender) {
        this.productFavoriteMapper = productFavoriteMapper;
        this.productMapper = productMapper;
        this.productFavoriteCacheService = productFavoriteCacheService;
        this.productHotEventSender = productHotEventSender;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long productId) {
        ensureProductActive(productId);

        ProductFavorite productFavorite = new ProductFavorite();
        productFavorite.setUserId(userId);
        productFavorite.setProductId(productId);
        productFavorite.setDeleted(NOT_DELETED);
        boolean favoriteCreated = true;
        try {
            productFavoriteMapper.insert(productFavorite);
        } catch (DuplicateKeyException exception) {
            // 同一用户重复收藏同一商品时保持幂等，唯一索引负责兜底防重。
            favoriteCreated = false;
        }
        boolean shouldSendHotEvent = favoriteCreated;
        runAfterCommit(() -> {
            refreshFavoriteCacheAfterCommit(userId, productId, true);
            if (shouldSendHotEvent) {
                sendHotEventQuietly(productId);
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelFavorite(Long userId, Long productId) {
        productFavoriteMapper.cancelFavorite(userId, productId);
        runAfterCommit(() -> refreshFavoriteCacheAfterCommit(userId, productId, false));
    }

    @Override
    public boolean isFavorited(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return false;
        }
        if (ensureFavoriteCacheLoaded(userId)) {
            try {
                return productFavoriteCacheService.contains(userId, productId);
            } catch (RuntimeException exception) {
                LOGGER.warn("query favorite cache failed, userId={}, productId={}", userId, productId, exception);
                invalidateFavoriteLoadedQuietly(userId);
            }
        }
        return isFavoritedFromDb(userId, productId);
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

        if (ensureFavoriteCacheLoaded(userId)) {
            try {
                return productFavoriteCacheService.batchContains(userId, distinctProductIds);
            } catch (RuntimeException exception) {
                LOGGER.warn("batch query favorite cache failed, userId={}", userId, exception);
                invalidateFavoriteLoadedQuietly(userId);
            }
        }

        Set<Long> favoritedProductIds = new HashSet<>(productFavoriteMapper.selectFavoritedProductIds(userId, distinctProductIds));
        result.replaceAll((productId, ignored) -> favoritedProductIds.contains(productId));
        return result;
    }

    @Override
    public List<ProductFavoriteVO> listFavorites(Long userId, Integer limit) {
        ensureFavoriteCacheLoaded(userId);
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

    private boolean ensureFavoriteCacheLoaded(Long userId) {
        try {
            if (productFavoriteCacheService.isLoaded(userId)) {
                return true;
            }
            List<Long> favoriteProductIds = productFavoriteMapper.selectFavoriteProductIdsByUser(userId);
            productFavoriteCacheService.rebuild(userId, favoriteProductIds);
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("rebuild favorite cache failed, userId={}", userId, exception);
            invalidateFavoriteLoadedQuietly(userId);
            return false;
        }
    }

    private boolean isFavoritedFromDb(Long userId, Long productId) {
        Long count = productFavoriteMapper.selectCount(new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getProductId, productId)
                .eq(ProductFavorite::getDeleted, NOT_DELETED));
        return count != null && count > 0;
    }

    private void refreshFavoriteCacheAfterCommit(Long userId, Long productId, boolean favorite) {
        try {
            if (favorite) {
                productFavoriteCacheService.addIfLoaded(userId, productId);
            } else {
                productFavoriteCacheService.removeIfLoaded(userId, productId);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("refresh favorite cache failed, userId={}, productId={}", userId, productId, exception);
            invalidateFavoriteLoadedQuietly(userId);
        }
    }

    private void invalidateFavoriteLoadedQuietly(Long userId) {
        try {
            productFavoriteCacheService.invalidateLoaded(userId);
        } catch (RuntimeException exception) {
            LOGGER.warn("invalidate favorite loaded cache failed, userId={}", userId, exception);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void sendHotEventQuietly(Long productId) {
        try {
            productHotEventSender.send(new ProductHotEvent(
                    UUID.randomUUID().toString(),
                    productId,
                    ProductHotAction.FAVORITE,
                    LocalDateTime.now()));
        } catch (RuntimeException exception) {
            LOGGER.warn("send favorite product hot event failed, productId={}", productId, exception);
        }
    }
}
