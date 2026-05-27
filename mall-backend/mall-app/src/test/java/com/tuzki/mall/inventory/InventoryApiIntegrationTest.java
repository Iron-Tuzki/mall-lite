package com.tuzki.mall.inventory;

import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.inventory.service.InventoryService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryService inventoryService;

    @Test
    void inventoryCrudMaintainsSkuStockBySkuId() throws Exception {
        Long skuId = insertSku();

        mockMvc.perform(post("/api/admin/inventories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "skuId": %d,
                                  "availableStock": 100,
                                  "lockedStock": 3
                                }
                                """.formatted(skuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skuId").value(skuId))
                .andExpect(jsonPath("$.data.availableStock").value(100))
                .andExpect(jsonPath("$.data.lockedStock").value(3))
                .andExpect(jsonPath("$.data.version").value(0));

        mockMvc.perform(get("/api/admin/inventories/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skuId").value(skuId))
                .andExpect(jsonPath("$.data.availableStock").value(100));

        mockMvc.perform(put("/api/admin/inventories/{skuId}", skuId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "availableStock": 80,
                                  "lockedStock": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableStock").value(80))
                .andExpect(jsonPath("$.data.lockedStock").value(5));

        mockMvc.perform(delete("/api/admin/inventories/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Inventory deletedInventory = inventoryMapper.selectList(null).stream()
                .filter(inventory -> skuId.equals(inventory.getSkuId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, deletedInventory.getDeleted());

        mockMvc.perform(get("/api/admin/inventories/{skuId}", skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("inventory not found"));
    }

    @Test
    void createInventoryRejectsMissingSkuAndDuplicateInventory() throws Exception {
        Long skuId = insertSku();

        mockMvc.perform(post("/api/admin/inventories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "skuId": 999999999,
                                  "availableStock": 100,
                                  "lockedStock": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("sku not found"));

        mockMvc.perform(post("/api/admin/inventories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "skuId": %d,
                                  "availableStock": 100,
                                  "lockedStock": 0
                                }
                                """.formatted(skuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/admin/inventories")
                        .contentType("application/json")
                        .content("""
                                {
                                  "skuId": %d,
                                  "availableStock": 200,
                                  "lockedStock": 0
                                }
                                """.formatted(skuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("inventory already exists"));
    }

    @Test
    void lockStockMovesAvailableStockToLockedStock() {
        Long skuId = insertSku();
        insertInventory(skuId, 10, 0);

        inventoryService.lockStock(skuId, 3);

        Inventory inventory = inventoryMapper.selectById(getInventoryIdBySkuId(skuId));
        assertEquals(7, inventory.getAvailableStock());
        assertEquals(3, inventory.getLockedStock());
        assertEquals(1, inventory.getVersion());
    }

    @Test
    void lockStockRejectsInvalidQuantityMissingInventoryAndInsufficientStock() {
        Long skuId = insertSku();
        insertInventory(skuId, 2, 0);

        assertBusinessException(400, "quantity must be greater than 0",
                () -> inventoryService.lockStock(skuId, 0));
        assertBusinessException(404, "inventory not found",
                () -> inventoryService.lockStock(999999999L, 1));
        assertBusinessException(400, "insufficient stock",
                () -> inventoryService.lockStock(skuId, 3));
    }

    private Long insertSku() {
        long suffix = System.nanoTime();

        Category category = new Category();
        category.setParentId(0L);
        category.setName("Inventory Category " + suffix);
        category.setLevel(1);
        category.setSort(1);
        category.setStatus(1);
        category.setDeleted(0);
        categoryMapper.insert(category);

        Product product = new Product();
        product.setCategoryId(category.getId());
        product.setProductCode("INV-P" + suffix);
        product.setName("Inventory Product " + suffix);
        product.setStatus(1);
        product.setSort(1);
        product.setDeleted(0);
        productMapper.insert(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode("INV-S" + suffix);
        sku.setSkuName("Inventory SKU " + suffix);
        sku.setPrice(new BigDecimal("99.00"));
        sku.setStatus(1);
        sku.setDeleted(0);
        skuMapper.insert(sku);
        return sku.getId();
    }

    private void insertInventory(Long skuId, Integer availableStock, Integer lockedStock) {
        Inventory inventory = new Inventory();
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(availableStock);
        inventory.setLockedStock(lockedStock);
        inventory.setVersion(0);
        inventory.setDeleted(0);
        inventoryMapper.insert(inventory);
    }

    private Long getInventoryIdBySkuId(Long skuId) {
        return inventoryMapper.selectList(null).stream()
                .filter(inventory -> skuId.equals(inventory.getSkuId()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void assertBusinessException(Integer code, String message, ThrowingAction action) {
        com.tuzki.mall.common.exception.BusinessException exception =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.tuzki.mall.common.exception.BusinessException.class, action::execute);
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @FunctionalInterface
    private interface ThrowingAction {

        void execute();
    }
}
