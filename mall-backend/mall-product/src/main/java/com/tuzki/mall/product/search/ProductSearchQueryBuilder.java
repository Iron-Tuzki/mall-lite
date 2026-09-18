package com.tuzki.mall.product.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 商品搜索 Query DSL 构建器，负责把商品搜索请求转换为 Elasticsearch JSON 查询体。
 */
public class ProductSearchQueryBuilder {

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 50;

    private static final String ON_SALE_STATUS = "ON_SALE";

    private static final String SORT_BY_RELEVANCE = "relevance";

    private static final String SORT_BY_SALES = "sales";

    private static final String SORT_BY_PRICE = "price";

    private static final String SORT_ORDER_ASC = "asc";

    private static final String SORT_ORDER_DESC = "desc";

    private final ObjectMapper objectMapper;

    public ProductSearchQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildSearchBody(ProductSearchRequest request) {
        ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
        ObjectNode root = objectMapper.createObjectNode();
        int pageNo = normalizePageNo(safeRequest.getPageNo());
        int pageSize = normalizePageSize(safeRequest.getPageSize());
        root.put("from", (pageNo - 1) * pageSize);
        root.put("size", pageSize);
        addSourceFields(root);
        addQuery(root, safeRequest);
        addAggregations(root);
        addSort(root, safeRequest);
        addHighlight(root, safeRequest);
        return toJson(root);
    }

    private void addSourceFields(ObjectNode root) {
        ArrayNode source = root.putArray("_source");
        source.add("productId");
        source.add("productName");
        source.add("categoryId");
        source.add("categoryName");
        source.add("brandName");
        source.add("price");
        source.add("sales");
        source.add("stock");
        source.add("mainImageUrl");
    }

    private void addQuery(ObjectNode root, ProductSearchRequest request) {
        ObjectNode bool = root.putObject("query").putObject("bool");
        ArrayNode must = bool.putArray("must");
        if (StringUtils.hasText(request.getKeyword())) {
            ObjectNode multiMatch = objectMapper.createObjectNode();
            multiMatch.put("query", request.getKeyword().trim());
            ArrayNode fields = multiMatch.putArray("fields");
            fields.add("productName^3");
            fields.add("brandName^2");
            fields.add("description");
            must.addObject().set("multi_match", multiMatch);
        }

        ArrayNode filter = bool.putArray("filter");
        filter.addObject().putObject("term").put("status", ON_SALE_STATUS);
        if (request.getCategoryId() != null) {
            filter.addObject().putObject("term").put("categoryId", request.getCategoryId());
        }
        if (StringUtils.hasText(request.getBrandName())) {
            filter.addObject().putObject("term").put("brandName", request.getBrandName().trim());
        }
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            ObjectNode range = filter.addObject().putObject("range").putObject("price");
            putDecimalIfPresent(range, "gte", request.getMinPrice());
            putDecimalIfPresent(range, "lte", request.getMaxPrice());
        }
    }

    private void addAggregations(ObjectNode root) {
        ObjectNode aggs = root.putObject("aggs");
        aggs.putObject("by_brand")
                .putObject("terms")
                .put("field", "brandName")
                .put("size", 20);
        aggs.putObject("by_category")
                .putObject("terms")
                .put("field", "categoryName")
                .put("size", 20);

        ArrayNode ranges = aggs.putObject("price_ranges")
                .putObject("range")
                .put("field", "price")
                .putArray("ranges");
        ranges.addObject().put("key", "100 元以下").put("to", 100);
        ranges.addObject().put("key", "100-500 元").put("from", 100).put("to", 500);
        ranges.addObject().put("key", "500 元以上").put("from", 500);
    }

    private void addSort(ObjectNode root, ProductSearchRequest request) {
        ArrayNode sort = root.putArray("sort");
        String sortBy = normalizeSortBy(request.getSortBy());
        String sortOrder = normalizeSortOrder(request.getSortOrder());
        if (SORT_BY_PRICE.equals(sortBy) || SORT_BY_SALES.equals(sortBy)) {
            sort.addObject().putObject(sortBy).put("order", sortOrder);
            sort.addObject().put("_score", SORT_ORDER_DESC);
            return;
        }
        sort.addObject().put("_score", "desc");
        sort.addObject().putObject("sales").put("order", SORT_ORDER_DESC);
        sort.addObject().putObject("price").put("order", SORT_ORDER_ASC);
    }

    private void addHighlight(ObjectNode root, ProductSearchRequest request) {
        if (!StringUtils.hasText(request.getKeyword())) {
            return;
        }
        ObjectNode highlight = root.putObject("highlight");
        highlight.putArray("pre_tags").add("<em>");
        highlight.putArray("post_tags").add("</em>");
        ObjectNode fields = highlight.putObject("fields");
        addHighlightField(fields, "productName");
        addHighlightField(fields, "brandName");
        addHighlightField(fields, "description");
    }

    private void addHighlightField(ObjectNode fields, String fieldName) {
        fields.putObject(fieldName)
                .put("fragment_size", 150)
                .put("number_of_fragments", 1);
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return SORT_BY_RELEVANCE;
        }
        String normalized = sortBy.trim();
        if (SORT_BY_PRICE.equals(normalized) || SORT_BY_SALES.equals(normalized)) {
            return normalized;
        }
        return SORT_BY_RELEVANCE;
    }

    private String normalizeSortOrder(String sortOrder) {
        if (!StringUtils.hasText(sortOrder)) {
            return SORT_ORDER_DESC;
        }
        String normalized = sortOrder.trim().toLowerCase();
        if (SORT_ORDER_ASC.equals(normalized)) {
            return SORT_ORDER_ASC;
        }
        return SORT_ORDER_DESC;
    }

    private void putDecimalIfPresent(ObjectNode node, String fieldName, BigDecimal value) {
        if (value != null) {
            node.put(fieldName, value);
        }
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < DEFAULT_PAGE_NO) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String toJson(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("build product search query body failed", exception);
        }
    }
}
