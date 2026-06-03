package com.tuzki.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.config.ProductHotProperties;
import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.service.ProductHotService;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 商品热点服务默认实现，基于 Redis 完成浏览去重、小时桶累加和首页热门榜单读取。
 */
@Service
public class ProductHotServiceImpl implements ProductHotService {

    private static final String VIEW_DEDUP_PREFIX = "mall:product:hot:view:dedup:";

    private static final String EVENT_PROCESSED_PREFIX = "mall:product:hot:event:processed:";

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final int DEFAULT_LIMIT = 10;

    private static final int MAX_LIMIT = 50;

    private final RedissonClient redissonClient;

    private final ProductHotEventSender productHotEventSender;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    private final ProductHotProperties properties;

    private final Clock clock;

    @Autowired
    public ProductHotServiceImpl(RedissonClient redissonClient,
                                 ProductHotEventSender productHotEventSender,
                                 ProductMapper productMapper,
                                 SkuMapper skuMapper,
                                 ProductHotProperties properties) {
        this(redissonClient, productHotEventSender, productMapper, skuMapper, properties, Clock.systemDefaultZone());
    }

    public ProductHotServiceImpl(RedissonClient redissonClient,
                                 ProductHotEventSender productHotEventSender,
                                 ProductMapper productMapper,
                                 SkuMapper skuMapper,
                                 ProductHotProperties properties,
                                 Clock clock) {
        this.redissonClient = redissonClient;
        this.productHotEventSender = productHotEventSender;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void recordViewByUser(Long userId, Long productId) {
        if (userId == null) {
            return;
        }
        recordView("user:" + userId, productId);
    }

    @Override
    public void recordViewByDevice(String deviceId, Long productId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        recordView("device:" + deviceId.trim(), productId);
    }

    @Override
    public void handleEvent(ProductHotEvent event) {
        if (event == null || event.eventId() == null || event.productId() == null || event.action() == null) {
            return;
        }
        // 事件eventId唯一，幂等控制，防止重复消费
        RBucket<String> processedBucket = redissonClient.getBucket(
                EVENT_PROCESSED_PREFIX + event.eventId(), StringCodec.INSTANCE);
        if (!processedBucket.setIfAbsent("1", Duration.ofHours(properties.getBucketTtlHours()))) {
            return;
        }
        LocalDateTime occurredAt = event.occurredAt() == null ? LocalDateTime.now() : event.occurredAt();
        // 根据时间点获取对应的时间缓存桶
        var hourBucket = redissonClient.getScoredSortedSet(hourKey(occurredAt), StringCodec.INSTANCE);
        hourBucket.addScore(String.valueOf(event.productId()), event.action().getScore()); // 累计本次操作分数
        hourBucket.expire(Duration.ofHours(properties.getBucketTtlHours()));
    }

    @Override
    public void aggregateHomepageHotProducts() {
        var temporaryBucket = redissonClient.getScoredSortedSet(properties.getTemporaryKey(), StringCodec.INSTANCE);
        temporaryBucket.delete();
        int total = temporaryBucket.union(buildAggregationWeights());
        int overflowCount = total - properties.getAggregationLimit();
        if (overflowCount > 0) {
            temporaryBucket.removeRangeByRank(0, overflowCount - 1);
        }
        temporaryBucket.expire(Duration.ofMinutes(properties.getHomepageTtlMinutes()));
        //聚合完成后再替换正式榜单，避免首页读取到半成品。
        temporaryBucket.rename(properties.getHomepageKey());
    }

    @Override
    public List<ProductSummaryVO> listHotProducts(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        try {
            var homepageBucket = redissonClient.<String>getScoredSortedSet(properties.getHomepageKey(), StringCodec.INSTANCE);
            Collection<ScoredEntry<String>> entries = homepageBucket.entryRangeReversed(0, safeLimit - 1);
            //Redis 访问异常时，直接使用现有推荐商品排序兜底
            if (entries.isEmpty()) {
                return fallbackRecommendProducts(safeLimit, Set.of());
            }
            List<Long> productIds = entries.stream()
                    .map(ScoredEntry::getValue)
                    .map(Long::valueOf)
                    .toList();
            List<ProductSummaryVO> result = buildProductSummariesInOrder(productIds);
            if (result.size() >= safeLimit) {
                return result;
            }
            // 当有效热门商品数量不足时，使用现有推荐商品排序补齐，并排除重复商品。
            Set<Long> existingProductIds = result.stream()
                    .map(ProductSummaryVO::getId)
                    .collect(Collectors.toSet());
            List<ProductSummaryVO> fallbackProducts = fallbackRecommendProducts(safeLimit - result.size(), existingProductIds);
            List<ProductSummaryVO> merged = new ArrayList<>(result);
            merged.addAll(fallbackProducts);
            return merged;
        } catch (RuntimeException exception) {
            return fallbackRecommendProducts(safeLimit, Set.of());
        }
    }

    private void recordView(String actor, Long productId) {
        if (productId == null) {
            return;
        }
        // 去重，五分钟内的重复请求不统计
        RBucket<String> bucket = redissonClient.getBucket(VIEW_DEDUP_PREFIX + actor + ":" + productId, StringCodec.INSTANCE);
        if (!bucket.setIfAbsent("1", Duration.ofMinutes(properties.getViewDedupTtlMinutes()))) {
            return;
        }
        productHotEventSender.send(new ProductHotEvent(
                UUID.randomUUID().toString(),
                productId,
                ProductHotAction.VIEW,
                LocalDateTime.now(clock)));
    }

    private String hourKey(LocalDateTime occurredAt) {
        return properties.getHourKeyPrefix() + occurredAt.format(HOUR_FORMATTER);
    }

    /**
     * 构造时间权重，越久远，权重越低
     * @return
     */
    private Map<String, Double> buildAggregationWeights() {
        LocalDateTime currentHour = LocalDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
        Map<String, Double> weights = new LinkedHashMap<>();
        for (int hourOffset = 0; hourOffset < properties.getWindowHours(); hourOffset++) {
            weights.put(hourKey(currentHour.minusHours(hourOffset)), Math.pow(0.9D, hourOffset));
        }
        return weights;
    }

    private List<ProductSummaryVO> buildProductSummariesInOrder(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Product> products = queryProducts(productIds);
        Map<Long, BigDecimal> minPrices = queryMinPrices(productIds);
        return productIds.stream()
                .map(products::get)
                .filter(Objects::nonNull)
                .map(product -> toProductSummaryVO(product, minPrices.get(product.getId())))
                .toList();
    }

    private List<ProductSummaryVO> fallbackRecommendProducts(int limit, Set<Long> excludedProductIds) {
        if (limit <= 0) {
            return List.of();
        }
        LambdaQueryWrapper<Product> queryWrapper = activeProductQuery()
                .orderByAsc(Product::getSort)
                .orderByDesc(Product::getId)
                .last("LIMIT " + limit);
        if (!excludedProductIds.isEmpty()) {
            queryWrapper.notIn(Product::getId, excludedProductIds);
        }
        List<Product> products = productMapper.selectList(queryWrapper);
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();
        Map<Long, BigDecimal> minPrices = queryMinPrices(productIds);
        return products.stream()
                .filter(this::isActive)
                .map(product -> toProductSummaryVO(product, minPrices.get(product.getId())))
                .limit(limit)
                .toList();
    }

    private Map<Long, Product> queryProducts(List<Long> productIds) {
        List<Product> products = productMapper.selectList(activeProductQuery()
                .in(Product::getId, productIds));
        Map<Long, Product> result = new LinkedHashMap<>();
        products.stream()
                .filter(this::isActive)
                .forEach(product -> result.put(product.getId(), product));
        return result;
    }

    private Map<Long, BigDecimal> queryMinPrices(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        List<Sku> skus = skuMapper.selectList(new LambdaQueryWrapper<Sku>()
                .in(Sku::getProductId, productIds)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED)
                .orderByAsc(Sku::getPrice));
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        skus.stream()
                .filter(this::isActive)
                .forEach(sku -> result.putIfAbsent(sku.getProductId(), sku.getPrice()));
        return result;
    }

    private LambdaQueryWrapper<Product> activeProductQuery() {
        return new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED);
    }

    private ProductSummaryVO toProductSummaryVO(Product product, BigDecimal minPrice) {
        ProductSummaryVO productSummaryVO = new ProductSummaryVO();
        productSummaryVO.setId(product.getId());
        productSummaryVO.setCategoryId(product.getCategoryId());
        productSummaryVO.setProductCode(product.getProductCode());
        productSummaryVO.setName(product.getName());
        productSummaryVO.setSubtitle(product.getSubtitle());
        productSummaryVO.setMainImageUrl(product.getMainImageUrl());
        productSummaryVO.setMinPrice(minPrice);
        return productSummaryVO;
    }

    private boolean isActive(Product product) {
        return product != null
                && product.getStatus() != null
                && product.getStatus() == ACTIVE_STATUS
                && product.getDeleted() != null
                && product.getDeleted() == NOT_DELETED;
    }

    private boolean isActive(Sku sku) {
        return sku != null
                && sku.getStatus() != null
                && sku.getStatus() == ACTIVE_STATUS
                && sku.getDeleted() != null
                && sku.getDeleted() == NOT_DELETED;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, Math.min(MAX_LIMIT, properties.getAggregationLimit()));
    }
}
