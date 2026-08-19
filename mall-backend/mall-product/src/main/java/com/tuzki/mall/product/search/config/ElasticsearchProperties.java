package com.tuzki.mall.product.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch 配置属性，负责承载商品搜索所需的连接地址、索引名和超时时间。
 */
@ConfigurationProperties(prefix = "mall.elasticsearch")
public class ElasticsearchProperties {

    private List<String> uris = new ArrayList<>(List.of("http://localhost:9200"));

    private String productIndex = "mall_product_agg_v1";

    private int connectTimeoutMillis = 3000;

    private int socketTimeoutMillis = 5000;

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getProductIndex() {
        return productIndex;
    }

    public void setProductIndex(String productIndex) {
        this.productIndex = productIndex;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public void setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
    }
}
