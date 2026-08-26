package com.tuzki.mall.admin;

import com.tuzki.mall.admin.product.search.ProductSearchIndexDocument;
import com.tuzki.mall.admin.product.search.ProductSearchIndexDocumentBuilder;
import com.tuzki.mall.inventory.entity.Inventory;
import com.tuzki.mall.inventory.mapper.InventoryMapper;
import com.tuzki.mall.product.entity.Category;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.CategoryMapper;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductSearchIndexDocumentBuilderTest {

    @Test
    void buildCreatesSearchDocumentFromProductCategorySkuAndInventory() {
        ProductMapper productMapper = mock(ProductMapper.class);
        CategoryMapper categoryMapper = mock(CategoryMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        InventoryMapper inventoryMapper = mock(InventoryMapper.class);
        ProductSearchIndexDocumentBuilder builder = new ProductSearchIndexDocumentBuilder(
                productMapper, categoryMapper, skuMapper, inventoryMapper);

        when(productMapper.selectById(100L)).thenReturn(product());
        when(categoryMapper.selectById(20L)).thenReturn(category());
        when(skuMapper.selectList(any())).thenReturn(List.of(sku(1001L, "白色款", "199.90"),
                sku(1002L, "黑色款", "159.90")));
        when(inventoryMapper.selectList(any())).thenReturn(List.of(inventory(1001L, 8), inventory(1002L, 12)));

        Optional<ProductSearchIndexDocument> document = builder.build(100L);

        assertTrue(document.isPresent());
        assertEquals(100L, document.get().getProductId());
        assertEquals("无线蓝牙耳机", document.get().getProductName());
        assertEquals("SoundLite", document.get().getBrandName());
        assertEquals(20L, document.get().getCategoryId());
        assertEquals("数码影音", document.get().getCategoryName());
        assertEquals(new BigDecimal("159.90"), document.get().getPrice());
        assertEquals(20, document.get().getStock());
        assertEquals("ON_SALE", document.get().getStatus());
    }

    private Product product() {
        Product product = new Product();
        product.setId(100L);
        product.setCategoryId(20L);
        product.setProductCode("EARPHONE-100");
        product.setName("无线蓝牙耳机");
        product.setBrandName("SoundLite");
        product.setSubtitle("主动降噪");
        product.setMainImageUrl("/images/earphone.png");
        product.setDescription("适合通勤的无线蓝牙耳机");
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }

    private Category category() {
        Category category = new Category();
        category.setId(20L);
        category.setName("数码影音");
        category.setStatus(1);
        category.setDeleted(0);
        return category;
    }

    private Sku sku(Long id, String name, String price) {
        Sku sku = new Sku();
        sku.setId(id);
        sku.setProductId(100L);
        sku.setSkuName(name);
        sku.setPrice(new BigDecimal(price));
        sku.setStatus(1);
        sku.setDeleted(0);
        return sku;
    }

    private Inventory inventory(Long skuId, Integer availableStock) {
        Inventory inventory = new Inventory();
        inventory.setSkuId(skuId);
        inventory.setAvailableStock(availableStock);
        inventory.setDeleted(0);
        return inventory;
    }
}
