package com.tuzki.mall.product;

import com.tuzki.mall.product.hot.ProductHotAction;
import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.hot.ProductHotEventSender;
import com.tuzki.mall.product.config.ProductHotProperties;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.product.service.impl.ProductHotServiceImpl;
import com.tuzki.mall.product.vo.ProductSummaryVO;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商品热点服务测试，验证浏览去重、热点累加、聚合和热门商品查询行为。
 */
class ProductHotServiceImplTest {

    @Test
    void repeatedViewBySameUserWithinDedupWindowOnlySendsOneEvent() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> dedupBucket = mock(RBucket.class);
        ProductHotEventSender eventSender = mock(ProductHotEventSender.class);
        when(redissonClient.<String>getBucket("mall:product:hot:view:dedup:user:1:100", org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(dedupBucket);
        when(dedupBucket.setIfAbsent("1", java.time.Duration.ofMinutes(5)))
                .thenReturn(true)
                .thenReturn(false);

        ProductHotServiceImpl service = new ProductHotServiceImpl(
                redissonClient,
                eventSender,
                mock(ProductMapper.class),
                mock(SkuMapper.class),
                hotProperties());

        service.recordViewByUser(1L, 100L);
        service.recordViewByUser(1L, 100L);

        verify(eventSender, times(1)).send(argThat(event ->
                event.productId().equals(100L) && event.action() == ProductHotAction.VIEW));
    }

    @Test
    void duplicatedEventOnlyIncrementsHourBucketOnce() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RBucket<String> processedBucket = mock(RBucket.class);
        RScoredSortedSet<String> hourBucket = mock(RScoredSortedSet.class);
        when(redissonClient.<String>getBucket("mall:product:hot:event:processed:event-1", org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(processedBucket);
        when(redissonClient.<String>getScoredSortedSet("mall:product:hot:hour:2026060308", org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(hourBucket);
        when(processedBucket.setIfAbsent("1", Duration.ofHours(48)))
                .thenReturn(true)
                .thenReturn(false);

        ProductHotServiceImpl service = new ProductHotServiceImpl(
                redissonClient,
                mock(ProductHotEventSender.class),
                mock(ProductMapper.class),
                mock(SkuMapper.class),
                hotProperties());
        ProductHotEvent event = new ProductHotEvent(
                "event-1",
                100L,
                ProductHotAction.CART_ADD,
                LocalDateTime.of(2026, 6, 3, 8, 15));

        service.handleEvent(event);
        service.handleEvent(event);

        verify(hourBucket, times(1)).addScore("100", 6);
        verify(hourBucket, times(1)).expire(Duration.ofHours(48));
    }

    @Test
    void aggregateHomepageHotProductsUsesWeightedRecentTwentyFourHourBuckets() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RScoredSortedSet<String> temporaryBucket = mock(RScoredSortedSet.class);
        ProductHotProperties properties = hotProperties();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-03T00:30:00Z"), ZoneId.of("Asia/Shanghai"));
        when(redissonClient.<String>getScoredSortedSet(properties.getTemporaryKey(), org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(temporaryBucket);
        when(temporaryBucket.union(anyMap())).thenReturn(60);

        ProductHotServiceImpl service = new ProductHotServiceImpl(
                redissonClient,
                mock(ProductHotEventSender.class),
                mock(ProductMapper.class),
                mock(SkuMapper.class),
                properties,
                fixedClock);

        service.aggregateHomepageHotProducts();

        verify(temporaryBucket).delete();
        verify(temporaryBucket).union(org.mockito.ArgumentMatchers.<Map<String, Double>>argThat(weights ->
                weights.size() == 24
                        && weights.get("mall:product:hot:hour:2026060308") == 1.0D
                        && Math.abs(weights.get("mall:product:hot:hour:2026060307") - 0.9D) < 0.0001D
                        && Math.abs(weights.get("mall:product:hot:hour:2026060209") - Math.pow(0.9D, 23)) < 0.0001D));
        verify(temporaryBucket).removeRangeByRank(0, 9);
        verify(temporaryBucket).expire(Duration.ofMinutes(15));
        verify(temporaryBucket).rename(properties.getHomepageKey());
    }

    @Test
    void aggregateHomepageHotProductsDeletesHomepageBucketWhenRecentBucketsAreEmpty() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RScoredSortedSet<String> temporaryBucket = mock(RScoredSortedSet.class);
        RScoredSortedSet<String> homepageBucket = mock(RScoredSortedSet.class);
        ProductHotProperties properties = hotProperties();
        when(redissonClient.<String>getScoredSortedSet(properties.getTemporaryKey(), org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(temporaryBucket);
        when(redissonClient.<String>getScoredSortedSet(properties.getHomepageKey(), org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(homepageBucket);
        when(temporaryBucket.union(anyMap())).thenReturn(0);

        ProductHotServiceImpl service = new ProductHotServiceImpl(
                redissonClient,
                mock(ProductHotEventSender.class),
                mock(ProductMapper.class),
                mock(SkuMapper.class),
                properties);

        service.aggregateHomepageHotProducts();

        verify(homepageBucket).delete();
        verify(temporaryBucket, never()).expire(any(Duration.class));
        verify(temporaryBucket, never()).rename(properties.getHomepageKey());
    }

    @Test
    void listHotProductsKeepsRedisRankOrderAndFillsMinPrice() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RScoredSortedSet<String> homepageBucket = mock(RScoredSortedSet.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        ProductHotProperties properties = hotProperties();
        when(redissonClient.<String>getScoredSortedSet(properties.getHomepageKey(), org.redisson.client.codec.StringCodec.INSTANCE))
                .thenReturn(homepageBucket);
        when(homepageBucket.entryRangeReversed(0, 1)).thenReturn(List.of(
                new ScoredEntry<>(80D, "100"),
                new ScoredEntry<>(60D, "200")
        ));
        when(productMapper.selectList(any())).thenReturn(List.of(
                product(200L, "P200", "Second Hot"),
                product(100L, "P100", "First Hot")
        ));
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(100L, new BigDecimal("9.90")),
                sku(200L, new BigDecimal("19.90"))
        ));
        ProductHotServiceImpl service = new ProductHotServiceImpl(
                redissonClient,
                mock(ProductHotEventSender.class),
                productMapper,
                skuMapper,
                properties);

        List<ProductSummaryVO> result = service.listHotProducts(2);

        assertThat(result).extracting(ProductSummaryVO::getId).containsExactly(100L, 200L);
        assertThat(result).extracting(ProductSummaryVO::getMinPrice)
                .containsExactly(new BigDecimal("9.90"), new BigDecimal("19.90"));
    }

    private ProductHotProperties hotProperties() {
        ProductHotProperties properties = new ProductHotProperties();
        properties.setHourKeyPrefix("mall:product:hot:hour:");
        properties.setHomepageKey("mall:product:hot:homepage");
        properties.setTemporaryKey("mall:product:hot:homepage:tmp");
        properties.setBucketTtlHours(48);
        properties.setHomepageTtlMinutes(15);
        properties.setViewDedupTtlMinutes(5);
        properties.setAggregationLimit(50);
        properties.setWindowHours(24);
        return properties;
    }

    private Product product(Long productId, String productCode, String name) {
        Product product = new Product();
        product.setId(productId);
        product.setCategoryId(1L);
        product.setProductCode(productCode);
        product.setName(name);
        product.setSubtitle("Hot product");
        product.setMainImageUrl("https://example.com/" + productId + ".png");
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }

    private Sku sku(Long productId, BigDecimal price) {
        Sku sku = new Sku();
        sku.setProductId(productId);
        sku.setPrice(price);
        sku.setStatus(1);
        sku.setDeleted(0);
        return sku;
    }
}
