package com.tuzki.mall.product.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSearchQueryBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildSearchBodyUsesKeywordFiltersAggregationsAndStableSort() throws Exception {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("无线 键盘");
        request.setCategoryId(20L);
        request.setBrandName("KeyMaster");
        request.setMinPrice(new BigDecimal("100.00"));
        request.setMaxPrice(new BigDecimal("800.00"));
        request.setPageNo(2);
        request.setPageSize(5);

        JsonNode root = objectMapper.readTree(new ProductSearchQueryBuilder(objectMapper).buildSearchBody(request));

        assertEquals(5, root.path("from").asInt());
        assertEquals(5, root.path("size").asInt());
        assertEquals("无线 键盘", root.path("query").path("bool").path("must").get(0)
                .path("multi_match").path("query").asText());
        assertEquals("productName^3", root.path("query").path("bool").path("must").get(0)
                .path("multi_match").path("fields").get(0).asText());
        assertEquals("brandName^2", root.path("query").path("bool").path("must").get(0)
                .path("multi_match").path("fields").get(1).asText());
        assertEquals("ON_SALE", root.path("query").path("bool").path("filter").get(0)
                .path("term").path("status").asText());
        assertEquals(20L, root.path("query").path("bool").path("filter").get(1)
                .path("term").path("categoryId").asLong());
        assertEquals("KeyMaster", root.path("query").path("bool").path("filter").get(2)
                .path("term").path("brandName").asText());
        assertEquals(100.00D, root.path("query").path("bool").path("filter").get(3)
                .path("range").path("price").path("gte").asDouble());
        assertEquals(800.00D, root.path("query").path("bool").path("filter").get(3)
                .path("range").path("price").path("lte").asDouble());
        assertEquals("brandName", root.path("aggs").path("by_brand").path("terms").path("field").asText());
        assertEquals("categoryName", root.path("aggs").path("by_category").path("terms").path("field").asText());
        assertEquals("price", root.path("aggs").path("price_ranges").path("range").path("field").asText());
        assertEquals("desc", root.path("sort").get(0).path("_score").asText());
        assertEquals("desc", root.path("sort").get(1).path("sales").asText());
        assertEquals("asc", root.path("sort").get(2).path("price").asText());
    }

    @Test
    void buildSearchBodyUsesMatchAllWhenKeywordIsBlankAndNormalizesPage() throws Exception {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword(" ");
        request.setPageNo(0);
        request.setPageSize(200);

        JsonNode root = objectMapper.readTree(new ProductSearchQueryBuilder(objectMapper).buildSearchBody(request));

        assertEquals(0, root.path("from").asInt());
        assertEquals(50, root.path("size").asInt());
        assertEquals(0, root.path("query").path("bool").path("must").size());
        assertEquals("ON_SALE", root.path("query").path("bool").path("filter").get(0)
                .path("term").path("status").asText());
    }
}
