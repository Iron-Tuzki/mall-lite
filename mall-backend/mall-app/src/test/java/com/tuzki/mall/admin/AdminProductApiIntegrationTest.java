package com.tuzki.mall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class AdminProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Test
    void createProductCreatesSkuAndInventoryInOneRequest() throws Exception {
        long suffix = System.nanoTime();
        Long categoryId = insertCategory("Admin Product Category " + suffix);
        String productCode = "ADM-P" + suffix;
        String skuCode = "ADM-S" + suffix;

        mockMvc.perform(post("/api/admin/products")
                        .contentType("application/json")
                        .content("""
                                {
                                  "categoryId": %d,
                                  "productCode": "%s",
                                  "name": "Admin Product %d",
                                  "subtitle": "Admin product subtitle",
                                  "mainImageUrl": "/images/admin-product.png",
                                  "description": "Admin product description",
                                  "status": 1,
                                  "sort": 1,
                                  "skus": [
                                    {
                                      "skuCode": "%s",
                                      "skuName": "Admin SKU",
                                      "specData": "{\\"color\\":\\"black\\"}",
                                      "price": 199.90,
                                      "originalPrice": 299.90,
                                      "mainImageUrl": "/images/admin-sku.png",
                                      "status": 1,
                                      "availableStock": 50
                                    }
                                  ]
                                }
                                """.formatted(categoryId, productCode, suffix, skuCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value(productCode))
                .andExpect(jsonPath("$.data.skus[0].skuCode").value(skuCode))
                .andExpect(jsonPath("$.data.skus[0].availableStock").value(50))
                .andExpect(jsonPath("$.data.skus[0].lockedStock").value(0));

        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getSkuCode, skuCode));
        assertNotNull(sku);
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getSkuId, sku.getId()));
        assertNotNull(inventory);
        assertEquals(50, inventory.getAvailableStock());
        assertEquals(0, inventory.getLockedStock());
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
}
