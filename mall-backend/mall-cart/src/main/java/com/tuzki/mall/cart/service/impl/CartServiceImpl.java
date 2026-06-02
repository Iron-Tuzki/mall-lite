package com.tuzki.mall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.cart.dto.CartAddRequest;
import com.tuzki.mall.cart.dto.CartBatchDeleteRequest;
import com.tuzki.mall.cart.dto.CartQuantityUpdateRequest;
import com.tuzki.mall.cart.entity.CartItem;
import com.tuzki.mall.cart.mapper.CartItemMapper;
import com.tuzki.mall.cart.message.CartChangeMessage;
import com.tuzki.mall.cart.message.CartChangeMessageSender;
import com.tuzki.mall.cart.message.CartChangeOperation;
import com.tuzki.mall.cart.service.CartCacheItem;
import com.tuzki.mall.cart.service.CartCacheMutation;
import com.tuzki.mall.cart.service.CartCacheService;
import com.tuzki.mall.cart.service.CartService;
import com.tuzki.mall.cart.vo.CartItemVO;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物车业务默认实现，编排 Redis 实时状态、RabbitMQ 变更消息和商品最新信息查询。
 */
@Service
public class CartServiceImpl implements CartService {

    private static final int ACTIVE_STATUS = 1;
    private static final int NOT_DELETED = 0;

    private final CartCacheService cartCacheService;
    private final CartItemMapper cartItemMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final CartChangeMessageSender cartChangeMessageSender;

    public CartServiceImpl(CartCacheService cartCacheService,
                           CartItemMapper cartItemMapper,
                           SkuMapper skuMapper,
                           ProductMapper productMapper,
                           CartChangeMessageSender cartChangeMessageSender) {
        this.cartCacheService = cartCacheService;
        this.cartItemMapper = cartItemMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.cartChangeMessageSender = cartChangeMessageSender;
    }

    @Override
    public void add(Long userId, CartAddRequest request) {
        ensureCacheLoaded(userId);
        Sku sku = getSku(request.getSkuId());
        getProduct(sku.getProductId());
        try {
            // sugus:添加到缓存中，然后发布消息
            publish(userId, request.getSkuId(), cartCacheService.add(userId, request.getSkuId(), request.getQuantity()),
                    CartChangeOperation.UPSERT);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, exception.getMessage());
        }
    }

    @Override
    public List<CartItemVO> list(Long userId) {
        ensureCacheLoaded(userId);
        List<CartItemVO> result = new ArrayList<>();
        cartCacheService.getAll(userId).forEach((skuId, cacheItem) -> {
            if (!cacheItem.deleted()) {
                result.add(toVO(skuId, cacheItem));
            }
        });
        return result;
    }

    @Override
    public void updateQuantity(Long userId, Long skuId, CartQuantityUpdateRequest request) {
        ensureCacheLoaded(userId);
        ensureActiveCartItem(userId, skuId);
        publish(userId, skuId, cartCacheService.mutate(userId, skuId, request.getQuantity(), false),
                CartChangeOperation.UPSERT);
    }

    @Override
    public void delete(Long userId, Long skuId) {
        ensureCacheLoaded(userId);
        ensureActiveCartItem(userId, skuId);
        publish(userId, skuId, cartCacheService.mutate(userId, skuId, 0, true), CartChangeOperation.DELETE);
    }

    @Override
    public void batchDelete(Long userId, CartBatchDeleteRequest request) {
        ensureCacheLoaded(userId);
        request.getSkuIds().stream().distinct().forEach(skuId -> {
            CartCacheItem item = cartCacheService.getAll(userId).get(skuId);
            if (item == null || item.deleted()) {
                return;
            }
            publish(userId, skuId, cartCacheService.mutate(userId, skuId, 0, true), CartChangeOperation.DELETE);
        });
    }

    private void publish(Long userId, Long skuId, CartCacheMutation mutation, CartChangeOperation operation) {
        CartCacheItem current = mutation.current();
        try {
            cartChangeMessageSender.send(new CartChangeMessage(
                    userId, skuId, current.quantity(), current.version(), operation));
        } catch (RuntimeException exception) {
            // 消息发送失败，使用旧版本数据回滚缓存
            cartCacheService.rollback(userId, skuId, current.version(), mutation.previousJson());
            throw new BusinessException(503, "cart change message send failed");
        }
    }

    private void ensureCacheLoaded(Long userId) {
        if (cartCacheService.isLoaded(userId)) {
            return;
        }
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId));
        cartCacheService.rebuild(userId, items);
    }

    private void ensureActiveCartItem(Long userId, Long skuId) {
        CartCacheItem item = cartCacheService.getAll(userId).get(skuId);
        if (item == null || item.deleted()) {
            throw new BusinessException(404, "cart item not found");
        }
    }

    private CartItemVO toVO(Long skuId, CartCacheItem cacheItem) {
        Sku sku = skuMapper.selectById(skuId);
        Product product = sku == null ? null : productMapper.selectById(sku.getProductId());
        CartItemVO vo = new CartItemVO();
        vo.setSkuId(skuId);
        vo.setQuantity(cacheItem.quantity());
        if (sku == null) {
            vo.setAvailable(false);
            return vo;
        }
        vo.setProductId(sku.getProductId());
        vo.setSkuName(sku.getSkuName());
        vo.setPrice(sku.getPrice());
        vo.setMainImageUrl(sku.getMainImageUrl());
        if (product != null) {
            vo.setProductName(product.getName());
            if (vo.getMainImageUrl() == null || vo.getMainImageUrl().isBlank()) {
                vo.setMainImageUrl(product.getMainImageUrl());
            }
        }
        vo.setAvailable(isActive(sku.getStatus(), sku.getDeleted()) && product != null
                && isActive(product.getStatus(), product.getDeleted()));
        return vo;
    }

    private Sku getSku(Long skuId) {
        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        return sku;
    }

    private Product getProduct(Long productId) {
        Product product = productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED));
        if (product == null) {
            throw new BusinessException(404, "product not found");
        }
        return product;
    }

    private boolean isActive(Integer status, Integer deleted) {
        return status != null && status == ACTIVE_STATUS && deleted != null && deleted == NOT_DELETED;
    }
}
