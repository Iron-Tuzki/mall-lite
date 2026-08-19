package com.tuzki.mall.product.search.service.impl;

import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.search.ProductSearchQueryBuilder;
import com.tuzki.mall.product.search.ProductSearchResultMapper;
import com.tuzki.mall.product.search.config.ElasticsearchProperties;
import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import com.tuzki.mall.product.search.service.ProductSearchService;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Elasticsearch 商品搜索服务实现，使用低层 REST Client 发送 Query DSL 并解析搜索结果。
 */
@Service
public class ElasticsearchProductSearchService implements ProductSearchService {

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 50;

    private final RestClient restClient;

    private final ElasticsearchProperties properties;

    private final ProductSearchQueryBuilder queryBuilder;

    private final ProductSearchResultMapper resultMapper;

    public ElasticsearchProductSearchService(RestClient restClient,
                                             ElasticsearchProperties properties,
                                             ProductSearchQueryBuilder queryBuilder,
                                             ProductSearchResultMapper resultMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.queryBuilder = queryBuilder;
        this.resultMapper = resultMapper;
    }

    @Override
    public ProductSearchResultVO search(ProductSearchRequest request) {
        ProductSearchRequest safeRequest = request == null ? new ProductSearchRequest() : request;
        Request elasticsearchRequest = new Request("GET", "/" + properties.getProductIndex() + "/_search");
        elasticsearchRequest.setJsonEntity(queryBuilder.buildSearchBody(safeRequest));
        try {
            Response response = restClient.performRequest(elasticsearchRequest);
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return resultMapper.map(responseBody, normalizePageNo(safeRequest.getPageNo()),
                    normalizePageSize(safeRequest.getPageSize()));
        } catch (IOException exception) {
            throw new BusinessException(503, "elasticsearch search failed");
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
}
