package com.tuzki.mall.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.admin.product.search.ProductSearchIndexDocument;
import com.tuzki.mall.admin.product.search.ProductSearchIndexRequestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSearchIndexRequestFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProductSearchIndexRequestFactory requestFactory = new ProductSearchIndexRequestFactory(objectMapper);

    @Test
    void buildCreateIndexBodyDefinesSearchAndAggregationFields() throws Exception {
        JsonNode root = objectMapper.readTree(requestFactory.buildCreateIndexBody());

        assertEquals("text", root.path("mappings").path("properties").path("productName").path("type").asText());
        assertEquals("keyword", root.path("mappings").path("properties").path("productName")
                .path("fields").path("keyword").path("type").asText());
        assertEquals("keyword", root.path("mappings").path("properties").path("brandName").path("type").asText());
        assertEquals("keyword", root.path("mappings").path("properties").path("status").path("type").asText());
        assertEquals("scaled_float", root.path("mappings").path("properties").path("price").path("type").asText());
        assertEquals(100, root.path("mappings").path("properties").path("price").path("scaling_factor").asInt());
    }

    @Test
    void buildIndexDocumentBodySerializesProductDocument() throws Exception {
        JsonNode root = objectMapper.readTree(requestFactory.buildIndexDocumentBody(document()));

        assertEquals(100L, root.path("productId").asLong());
        assertEquals("无线蓝牙耳机", root.path("productName").asText());
        assertEquals("SoundLite", root.path("brandName").asText());
        assertEquals("数码影音", root.path("categoryName").asText());
        assertEquals("ON_SALE", root.path("status").asText());
        assertEquals("适合通勤的无线蓝牙耳机", root.path("description").asText());
    }

    @Test
    void buildBulkIndexBodyEndsWithNewlineForElasticsearchBulkApi() {
        String body = requestFactory.buildBulkIndexBody("mall_product_search_v1", java.util.List.of(document()));

        assertTrue(body.endsWith("\n"));
        assertTrue(body.contains("\"_index\":\"mall_product_search_v1\""));
        assertTrue(body.contains("\"_id\":\"100\""));
    }

    private ProductSearchIndexDocument document() {
        ProductSearchIndexDocument document = new ProductSearchIndexDocument();
        document.setProductId(100L);
        document.setProductName("无线蓝牙耳机");
        document.setProductCode("EARPHONE-100");
        document.setBrandName("SoundLite");
        document.setCategoryId(20L);
        document.setCategoryName("数码影音");
        document.setPrice(new BigDecimal("159.90"));
        document.setSales(0);
        document.setStock(20);
        document.setMainImageUrl("/images/earphone.png");
        document.setStatus("ON_SALE");
        document.setDescription("适合通勤的无线蓝牙耳机");
        return document;
    }
}
