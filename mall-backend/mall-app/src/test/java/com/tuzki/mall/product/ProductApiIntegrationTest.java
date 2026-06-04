package com.tuzki.mall.product;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private RedissonClient redissonClient;

    private Long cachedProductId;

    @BeforeEach
    void clearHotProductsBeforeTest() {
        redissonClient.getScoredSortedSet("mall:product:hot:homepage", StringCodec.INSTANCE).delete();
    }

    @AfterEach
    void clearProductDetailCache() {
        FlowRuleManager.loadRules(List.of());
        if (cachedProductId != null) {
            redissonClient.getBucket("mall:product:detail:" + cachedProductId).delete();
            redissonClient.getBucket("mall:product:hot:detail:" + cachedProductId).delete();
        }
        redissonClient.getScoredSortedSet("mall:product:hot:homepage", StringCodec.INSTANCE).delete();
    }

    @Test
    void missingProductDetailIsCachedWithShortTtl() throws Exception {
        Long missingProductId = 999999998L;
        cachedProductId = missingProductId;

        mockMvc.perform(get("/api/products/{productId}", missingProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("product not found"));

        String cacheKey = "mall:product:detail:" + missingProductId;
        assertEquals("__NULL__", redissonClient.getBucket(cacheKey, StringCodec.INSTANCE).get());
        long ttl = redissonClient.getBucket(cacheKey, StringCodec.INSTANCE).remainTimeToLive();
        assertTrue(ttl > 0 && ttl <= 300_000);

        Long categoryId = insertCategory("Null Cache Category");
        insertProductWithId(missingProductId, categoryId, "NULL" + System.nanoTime(), "Null Cache Product");

        mockMvc.perform(get("/api/products/{productId}", missingProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("product not found"));
    }

    @Test
    void productReadApisReturnActiveProductCatalogData() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Digital " + suffix);
        Long hiddenCategoryId = insertDisabledCategory("Hidden " + suffix);
        Long productId = insertProduct(categoryId, "P" + suffix, "Phone " + suffix);
        Long hiddenProductId = insertOfflineProduct(categoryId, "PX" + suffix, "Hidden Phone " + suffix);
        Long skuId = insertSku(productId, "S" + suffix, "Phone Black", new BigDecimal("1999.00"));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(categoryId))
                .andExpect(jsonPath("$.data[0].name").value("Digital " + suffix))
                .andExpect(jsonPath("$.data[?(@.id == %s)]".formatted(hiddenCategoryId)).doesNotExist());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(productId))
                .andExpect(jsonPath("$.data[0].name").value("Phone " + suffix))
                .andExpect(jsonPath("$.data[0].minPrice").value(1999.00))
                .andExpect(jsonPath("$.data[?(@.id == %s)]".formatted(hiddenProductId)).doesNotExist());

        mockMvc.perform(get("/api/products").param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].categoryId").value(categoryId));

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("Phone " + suffix))
                .andExpect(jsonPath("$.data.minPrice").value(1999.00))
                .andExpect(jsonPath("$.data.skus[0].id").value(skuId))
                .andExpect(jsonPath("$.data.skus[0].price").value(1999.00));

        mockMvc.perform(get("/api/skus/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(skuId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.skuName").value("Phone Black"));
    }

    @Test
    void productReadApisRejectMissingResources() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("product not found"));

        mockMvc.perform(get("/api/skus/{skuId}", 999999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("sku not found"));
    }

    @Test
    void recommendProductsReturnsPagedActiveProductSummaries() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Recommend " + suffix);
        Long firstProductId = insertProduct(categoryId, "R1" + suffix, "Recommend First " + suffix);
        Long secondProductId = insertProduct(categoryId, "R2" + suffix, "Recommend Second " + suffix);
        insertOfflineProduct(categoryId, "R3" + suffix, "Recommend Hidden " + suffix);
        insertSku(firstProductId, "RS1" + suffix, "First SKU", new BigDecimal("99.90"));
        insertSku(secondProductId, "RS2" + suffix, "Second SKU", new BigDecimal("88.80"));

        mockMvc.perform(get("/api/products/recommend")
                        .param("pageNo", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.records[0].id").value(secondProductId))
                .andExpect(jsonPath("$.data.records[0].minPrice").value(88.80))
                .andExpect(jsonPath("$.data.records[1].id").value(firstProductId))
                .andExpect(jsonPath("$.data.records[1].minPrice").value(99.90));
    }

    @Test
    void hotProductsReturnProductsByRedisHotRank() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Hot " + suffix);
        Long firstProductId = insertProduct(categoryId, "H1" + suffix, "Hot First " + suffix);
        Long secondProductId = insertProduct(categoryId, "H2" + suffix, "Hot Second " + suffix);
        insertSku(firstProductId, "HS1" + suffix, "Hot First SKU", new BigDecimal("199.90"));
        insertSku(secondProductId, "HS2" + suffix, "Hot Second SKU", new BigDecimal("299.90"));
        redissonClient.getScoredSortedSet("mall:product:hot:homepage", StringCodec.INSTANCE)
                .add(80D, String.valueOf(firstProductId));
        redissonClient.getScoredSortedSet("mall:product:hot:homepage", StringCodec.INSTANCE)
                .add(120D, String.valueOf(secondProductId));

        mockMvc.perform(get("/api/products/hot").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(secondProductId))
                .andExpect(jsonPath("$.data[0].minPrice").value(299.90))
                .andExpect(jsonPath("$.data[1].id").value(firstProductId))
                .andExpect(jsonPath("$.data[1].minPrice").value(199.90));
    }

    @Test
    void recommendProductsScrollReturnsNextBatchByCursor() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Scroll " + suffix);
        Long newestProductId = insertProduct(categoryId, "SC3" + suffix, "Scroll Newest " + suffix);
        Long middleProductId = insertProduct(categoryId, "SC2" + suffix, "Scroll Middle " + suffix);
        Long oldestProductId = insertProduct(categoryId, "SC1" + suffix, "Scroll Oldest " + suffix);
        insertSku(newestProductId, "SCS3" + suffix, "Newest SKU", new BigDecimal("33.30"));
        insertSku(middleProductId, "SCS2" + suffix, "Middle SKU", new BigDecimal("22.20"));
        insertSku(oldestProductId, "SCS1" + suffix, "Oldest SKU", new BigDecimal("11.10"));

        mockMvc.perform(get("/api/products/recommend/scroll")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records[0].id").value(oldestProductId))
                .andExpect(jsonPath("$.data.records[1].id").value(middleProductId))
                .andExpect(jsonPath("$.data.nextSort").value(1))
                .andExpect(jsonPath("$.data.nextId").value(middleProductId))
                .andExpect(jsonPath("$.data.hasMore").value(true));

        mockMvc.perform(get("/api/products/recommend/scroll")
                        .param("pageSize", "2")
                        .param("lastSort", "1")
                        .param("lastId", middleProductId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.records[0].id").value(newestProductId))
                .andExpect(jsonPath("$.data.records[?(@.id == %s)]".formatted(middleProductId)).doesNotExist());
    }

    @Test
    void productDetailApiReadsFromCacheAfterFirstQuery() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Cache " + suffix);
        Long productId = insertProduct(categoryId, "C" + suffix, "Cache Product " + suffix);
        cachedProductId = productId;
        insertSku(productId, "CS" + suffix, "Cache SKU", new BigDecimal("66.60"));

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Cache Product " + suffix));

        Product product = productMapper.selectById(productId);
        product.setName("Changed Product " + suffix);
        productMapper.updateById(product);

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Cache Product " + suffix));
    }

    @Test
    void hotProductDetailApiWritesIndependentHotDetailCache() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Hot Detail " + suffix);
        Long productId = insertProduct(categoryId, "HD" + suffix, "Hot Detail Product " + suffix);
        cachedProductId = productId;
        insertSku(productId, "HDS" + suffix, "Hot Detail SKU", new BigDecimal("77.70"));

        mockMvc.perform(get("/api/products/hot/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("Hot Detail Product " + suffix))
                .andExpect(jsonPath("$.data.minPrice").value(77.70));

        String cacheValue = redissonClient.<String>getBucket(
                "mall:product:hot:detail:" + productId, StringCodec.INSTANCE).get();
        assertTrue(cacheValue != null && cacheValue.contains("Hot Detail Product " + suffix));
    }

    @Test
    void hotProductDetailApiReturnsBusyResultWhenSentinelFlowControlTriggered() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Hot Sentinel " + suffix);
        Long productId = insertProduct(categoryId, "HSN" + suffix, "Hot Sentinel Product " + suffix);
        cachedProductId = productId;
        insertSku(productId, "HSNS" + suffix, "Hot Sentinel SKU", new BigDecimal("88.80"));
        FlowRule flowRule = new FlowRule("hot-product-detail");
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(0);
        FlowRuleManager.loadRules(List.of(flowRule));

        mockMvc.perform(get("/api/products/hot/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("hot product detail is busy"));
    }

    private Long insertCategory(String name) {
        Category category = new Category();
        category.setParentId(0L);
        category.setName(name);
        category.setLevel(1);
        category.setSort(1);
        category.setStatus(1);
        category.setDeleted(0);
        categoryMapper.insert(category);
        return category.getId();
    }

    private Long insertDisabledCategory(String name) {
        Category category = new Category();
        category.setParentId(0L);
        category.setName(name);
        category.setLevel(1);
        category.setSort(2);
        category.setStatus(0);
        category.setDeleted(0);
        categoryMapper.insert(category);
        return category.getId();
    }

    private Long insertProduct(Long categoryId, String productCode, String name) {
        Product product = new Product();
        product.setCategoryId(categoryId);
        product.setProductCode(productCode);
        product.setName(name);
        product.setSubtitle("Lightweight test product");
        product.setDescription("Product description");
        product.setStatus(1);
        product.setSort(1);
        product.setDeleted(0);
        productMapper.insert(product);
        return product.getId();
    }

    private void insertProductWithId(Long productId, Long categoryId, String productCode, String name) {
        Product product = new Product();
        product.setId(productId);
        product.setCategoryId(categoryId);
        product.setProductCode(productCode);
        product.setName(name);
        product.setSubtitle("Null cache test product");
        product.setDescription("Null cache test description");
        product.setStatus(1);
        product.setSort(1);
        product.setDeleted(0);
        productMapper.insert(product);
    }

    private Long insertOfflineProduct(Long categoryId, String productCode, String name) {
        Product product = new Product();
        product.setCategoryId(categoryId);
        product.setProductCode(productCode);
        product.setName(name);
        product.setSubtitle("Hidden product");
        product.setDescription("Hidden description");
        product.setStatus(0);
        product.setSort(2);
        product.setDeleted(0);
        productMapper.insert(product);
        return product.getId();
    }

    private Long insertSku(Long productId, String skuCode, String skuName, BigDecimal price) {
        Sku sku = new Sku();
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSkuName(skuName);
        sku.setSpecData("{\"color\":\"black\"}");
        sku.setPrice(price);
        sku.setOriginalPrice(new BigDecimal("2199.00"));
        sku.setStatus(1);
        sku.setDeleted(0);
        skuMapper.insert(sku);
        return sku.getId();
    }
}
