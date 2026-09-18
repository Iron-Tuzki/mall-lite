package com.tuzki.mall.product.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.product.search.vo.ProductSearchAggVO;
import com.tuzki.mall.product.search.vo.ProductSearchItemVO;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品搜索结果映射器，负责把 Elasticsearch 搜索响应解析为前台商品搜索视图对象。
 */
public class ProductSearchResultMapper {

    private final ObjectMapper objectMapper;

    public ProductSearchResultMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ProductSearchResultVO map(String responseBody, int pageNo, int pageSize) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ProductSearchResultVO result = new ProductSearchResultVO();
            result.setPageNo(pageNo);
            result.setPageSize(pageSize);
            result.setTotal(root.path("hits").path("total").path("value").asLong());
            result.setRecords(mapRecords(root.path("hits").path("hits")));
            result.setBrandAggs(mapBuckets(root.path("aggregations").path("by_brand").path("buckets")));
            result.setCategoryAggs(mapBuckets(root.path("aggregations").path("by_category").path("buckets")));
            result.setPriceAggs(mapBuckets(root.path("aggregations").path("price_ranges").path("buckets")));
            return result;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("parse product search response failed", exception);
        }
    }

    private List<ProductSearchItemVO> mapRecords(JsonNode hits) {
        List<ProductSearchItemVO> records = new ArrayList<>();
        if (!hits.isArray()) {
            return records;
        }
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            ProductSearchItemVO item = new ProductSearchItemVO();
            item.setProductId(readLong(source, "productId", "id"));
            item.setProductName(readText(source, "productName", "name"));
            item.setCategoryId(readLong(source, "categoryId"));
            item.setCategoryName(readText(source, "categoryName"));
            item.setBrandName(readText(source, "brandName"));
            item.setPrice(readDecimal(source, "price", "minPrice"));
            item.setSales(readInteger(source, "sales"));
            item.setStock(readInteger(source, "stock"));
            item.setMainImageUrl(readText(source, "mainImageUrl"));
            item.setHighlights(mapHighlights(hit.path("highlight")));
            records.add(item);
        }
        return records;
    }

    private List<ProductSearchAggVO> mapBuckets(JsonNode buckets) {
        List<ProductSearchAggVO> aggs = new ArrayList<>();
        if (!buckets.isArray()) {
            return aggs;
        }
        for (JsonNode bucket : buckets) {
            ProductSearchAggVO agg = new ProductSearchAggVO();
            agg.setKey(bucket.path("key").asText());
            agg.setCount(bucket.path("doc_count").asLong());
            aggs.add(agg);
        }
        return aggs;
    }

    private Map<String, List<String>> mapHighlights(JsonNode highlight) {
        Map<String, List<String>> highlights = new LinkedHashMap<>();
        if (!highlight.isObject()) {
            return highlights;
        }
        highlight.properties().forEach(entry -> highlights.put(entry.getKey(), mapHighlightFragments(entry.getValue())));
        return highlights;
    }

    private List<String> mapHighlightFragments(JsonNode fragments) {
        List<String> values = new ArrayList<>();
        if (!fragments.isArray()) {
            return values;
        }
        for (JsonNode fragment : fragments) {
            values.add(fragment.asText());
        }
        return values;
    }

    private String readText(JsonNode source, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = source.get(fieldName);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private Long readLong(JsonNode source, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = source.get(fieldName);
            if (value != null && value.canConvertToLong()) {
                return value.asLong();
            }
        }
        return null;
    }

    private Integer readInteger(JsonNode source, String fieldName) {
        JsonNode value = source.get(fieldName);
        if (value == null || !value.canConvertToInt()) {
            return null;
        }
        return value.asInt();
    }

    private BigDecimal readDecimal(JsonNode source, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = source.get(fieldName);
            if (value != null && value.isNumber()) {
                return value.decimalValue();
            }
        }
        return null;
    }
}
