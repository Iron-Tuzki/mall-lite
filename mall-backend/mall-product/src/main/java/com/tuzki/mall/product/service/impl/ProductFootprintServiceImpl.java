package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.service.ProductFootprintService;
import com.tuzki.mall.product.vo.ProductFootprintVO;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 商品浏览足迹服务默认实现，使用 Redis ZSet 保存用户最近浏览商品及访问时间。
 */
@Service
public class ProductFootprintServiceImpl implements ProductFootprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductFootprintServiceImpl.class);

    private static final String FOOTPRINT_KEY_PREFIX = "mall:user:footprints:";

    private static final int MAX_FOOTPRINT_COUNT = 100;

    private static final Duration FOOTPRINT_TTL = Duration.ofDays(90);

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private final RedissonClient redissonClient;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    public ProductFootprintServiceImpl(RedissonClient redissonClient, ProductMapper productMapper, SkuMapper skuMapper) {
        this.redissonClient = redissonClient;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    @Override
    public void record(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return;
        }
        RScoredSortedSet<String> footprints = footprints(userId);
        footprints.add(System.currentTimeMillis(), String.valueOf(productId));
        int overflowCount = footprints.size() - MAX_FOOTPRINT_COUNT;
        if (overflowCount > 0) {
            footprints.removeRangeByRank(0, overflowCount - 1);
        }
        footprints.expire(FOOTPRINT_TTL);
    }

    @Override
    public List<ProductFootprintVO> listRecent(Long userId, Integer limit) {
        long begin = System.currentTimeMillis();
        int safeLimit = normalizeLimit(limit);
        Collection<ScoredEntry<String>> entries = footprints(userId).entryRangeReversed(0, safeLimit - 1);
        if (entries.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entries.stream()
                .map(entry -> Long.valueOf(entry.getValue()))
                .toList();
        Map<Long, Product> products = queryProducts(productIds);
        Map<Long, BigDecimal> minPrices = queryMinPrices(productIds);
        List<ProductFootprintVO> list = entries.stream()
                .map(entry -> toFootprintVO(entry, products, minPrices))
                .filter(Objects::nonNull)
                .toList();
        long end = System.currentTimeMillis();
        LOGGER.info("查询足迹耗时：{} ms" , (end - begin));
        return list;
    }

    @Override
    public void remove(Long userId, Long productId) {
        if (userId != null && productId != null) {
            footprints(userId).remove(String.valueOf(productId));
        }
    }

    @Override
    public void clear(Long userId) {
        if (userId != null) {
            footprints(userId).delete();
        }
    }

    private Map<Long, Product> queryProducts(List<Long> productIds) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .in(Product::getId, productIds)
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED));
        Map<Long, Product> result = new LinkedHashMap<>();
        products.forEach(product -> result.put(product.getId(), product));
        return result;
    }

    private Map<Long, BigDecimal> queryMinPrices(List<Long> productIds) {
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .in(Sku::getProductId, productIds)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED)
                .orderByAsc(Sku::getPrice));
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        skus.forEach(sku -> result.putIfAbsent(sku.getProductId(), sku.getPrice()));
        return result;
    }

    private ProductFootprintVO toFootprintVO(ScoredEntry<String> entry, Map<Long, Product> products,
                                             Map<Long, BigDecimal> minPrices) {
        Product product = products.get(Long.valueOf(entry.getValue()));
        if (product == null) {
            return null;
        }
        ProductFootprintVO footprintVO = new ProductFootprintVO();
        footprintVO.setProductId(product.getId());
        footprintVO.setName(product.getName());
        footprintVO.setSubtitle(product.getSubtitle());
        footprintVO.setMainImageUrl(product.getMainImageUrl());
        footprintVO.setMinPrice(minPrices.get(product.getId()));
        footprintVO.setBrowseTime(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(entry.getScore().longValue()),
                ZoneId.systemDefault()));
        return footprintVO;
    }

    private RScoredSortedSet<String> footprints(Long userId) {
        return redissonClient.getScoredSortedSet(FOOTPRINT_KEY_PREFIX + userId, StringCodec.INSTANCE);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return MAX_FOOTPRINT_COUNT;
        }
        return Math.min(limit, MAX_FOOTPRINT_COUNT);
    }
}
