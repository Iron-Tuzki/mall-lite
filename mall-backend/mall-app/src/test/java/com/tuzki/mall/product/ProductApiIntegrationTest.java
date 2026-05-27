package com.tuzki.mall.product;

import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
