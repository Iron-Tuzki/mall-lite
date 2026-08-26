package com.tuzki.mall.admin.product.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品搜索索引请求工厂，负责构建 Elasticsearch 建索引、写文档和批量写入所需的 JSON 请求体。
 */
@Component
public class ProductSearchIndexRequestFactory {

    private final ObjectMapper objectMapper;

    public ProductSearchIndexRequestFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildCreateIndexBody() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode properties = root.putObject("mappings").putObject("properties");
        properties.putObject("productId").put("type", "long");
        properties.putObject("productCode").put("type", "keyword");
        ObjectNode productName = properties.putObject("productName");
        productName.put("type", "text");
        productName.putObject("fields").putObject("keyword")
                .put("type", "keyword")
                .put("ignore_above", 256);
        properties.putObject("brandName").put("type", "keyword");
        properties.putObject("categoryId").put("type", "long");
        properties.putObject("categoryName").put("type", "keyword");
        properties.putObject("price").put("type", "scaled_float").put("scaling_factor", 100);
        properties.putObject("sales").put("type", "integer");
        properties.putObject("stock").put("type", "integer");
        properties.putObject("mainImageUrl").put("type", "keyword").put("index", false);
        properties.putObject("status").put("type", "keyword");
        properties.putObject("description").put("type", "text");
        return writeJson(root);
    }

    public String buildIndexDocumentBody(ProductSearchIndexDocument document) {
        return writeJson(document);
    }

    public String buildBulkIndexBody(String indexName, List<ProductSearchIndexDocument> documents) {
        StringBuilder body = new StringBuilder();
        for (ProductSearchIndexDocument document : documents) {
            ObjectNode action = objectMapper.createObjectNode();
            action.putObject("index")
                    .put("_index", indexName)
                    .put("_id", String.valueOf(document.getProductId()));
            body.append(writeJson(action)).append('\n');
            body.append(buildIndexDocumentBody(document)).append('\n');
        }
        return body.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to build product search index json", exception);
        }
    }
}
