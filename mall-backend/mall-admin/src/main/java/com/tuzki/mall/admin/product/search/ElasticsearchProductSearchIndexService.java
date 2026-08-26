package com.tuzki.mall.admin.product.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.search.config.ElasticsearchProperties;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Elasticsearch 商品搜索索引服务实现，负责创建索引、写入商品文档、删除商品文档和批量重建索引。
 */
@Service
public class ElasticsearchProductSearchIndexService implements ProductSearchIndexService {

    private static final int NOT_FOUND = 404;

    private static final int NOT_DELETED = 0;

    private final RestClient esRestClient;

    private final ElasticsearchProperties properties;

    private final ProductSearchIndexDocumentBuilder documentBuilder;

    private final ProductSearchIndexRequestFactory requestFactory;

    private final ProductMapper productMapper;

    public ElasticsearchProductSearchIndexService(RestClient esRestClient,
                                                  ElasticsearchProperties properties,
                                                  ProductSearchIndexDocumentBuilder documentBuilder,
                                                  ProductSearchIndexRequestFactory requestFactory,
                                                  ProductMapper productMapper) {
        this.esRestClient = esRestClient;
        this.properties = properties;
        this.documentBuilder = documentBuilder;
        this.requestFactory = requestFactory;
        this.productMapper = productMapper;
    }

    @Override
    public void createIndexIfAbsent() {
        String indexName = properties.getProductIndex();
        try {
            // 先查看es索引是否存在，不存在报错则建立索引
            esRestClient.performRequest(new Request("HEAD", "/" + indexName));
        } catch (ResponseException exception) {
            if (statusCode(exception) != NOT_FOUND) {
                throw elasticsearchException("check product search index failed", exception);
            }
            Request createIndexRequest = new Request("PUT", "/" + indexName);
            createIndexRequest.setJsonEntity(requestFactory.buildCreateIndexBody());
            perform(createIndexRequest, "create product search index failed");
        } catch (IOException exception) {
            throw elasticsearchException("check product search index failed", exception);
        }
    }

    @Override
    public void syncProduct(Long productId) {
        if (productId == null) {
            return;
        }
        documentBuilder.build(productId).ifPresentOrElse(document -> {
            createIndexIfAbsent();
            Request request = new Request("PUT", "/" + properties.getProductIndex() + "/_doc/" + productId);
            request.setJsonEntity(requestFactory.buildIndexDocumentBody(document));
            perform(request, "sync product search document failed");
        }, () -> deleteProduct(productId));
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productId == null) {
            return;
        }
        Request request = new Request("DELETE", "/" + properties.getProductIndex() + "/_doc/" + productId);
        try {
            esRestClient.performRequest(request);
        } catch (ResponseException exception) {
            if (statusCode(exception) != NOT_FOUND) {
                throw elasticsearchException("delete product search document failed", exception);
            }
        } catch (IOException exception) {
            throw elasticsearchException("delete product search document failed", exception);
        }
    }

    @Override
    public void rebuildAll() {
        createIndexIfAbsent();
        List<ProductSearchIndexDocument> documents = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .eq(Product::getDeleted, NOT_DELETED)
                        .orderByAsc(Product::getId))
                .stream()
                .map(product -> documentBuilder.build(product.getId()))
                .flatMap(java.util.Optional::stream)
                .toList();
        if (documents.isEmpty()) {
            return;
        }
        Request request = new Request("POST", "/_bulk");
        request.setJsonEntity(requestFactory.buildBulkIndexBody(properties.getProductIndex(), documents));
        perform(request, "rebuild product search index failed");
    }

    private void perform(Request request, String errorMessage) {
        try {
            esRestClient.performRequest(request);
        } catch (IOException exception) {
            throw elasticsearchException(errorMessage, exception);
        }
    }

    private int statusCode(ResponseException exception) {
        return exception.getResponse().getStatusLine().getStatusCode();
    }

    private BusinessException elasticsearchException(String message, Exception exception) {
        return new BusinessException(503, message);
    }
}
