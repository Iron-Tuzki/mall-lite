package com.tuzki.mall.product.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.product.search.ProductSearchQueryBuilder;
import com.tuzki.mall.product.search.ProductSearchResultMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Elasticsearch 客户端配置类，负责创建低层 REST Client 和商品搜索 Query DSL 构建器。
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    @Bean
    public RestClient elasticsearchRestClient(ElasticsearchProperties properties) {
        List<HttpHost> hosts = properties.getUris().stream()
                .map(HttpHost::create)
                .toList();
        return RestClient.builder(hosts.toArray(HttpHost[]::new))
                .setRequestConfigCallback(requestConfig -> requestConfig
                        .setConnectTimeout(properties.getConnectTimeoutMillis())
                        .setSocketTimeout(properties.getSocketTimeoutMillis()))
                .build();
    }

    @Bean
    public ProductSearchQueryBuilder productSearchQueryBuilder(ObjectMapper objectMapper) {
        return new ProductSearchQueryBuilder(objectMapper);
    }

    @Bean
    public ProductSearchResultMapper productSearchResultMapper(ObjectMapper objectMapper) {
        return new ProductSearchResultMapper(objectMapper);
    }
}
