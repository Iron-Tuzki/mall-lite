package com.tuzki.mall.admin;

import com.tuzki.mall.admin.product.search.ElasticsearchProductSearchIndexService;
import com.tuzki.mall.admin.product.search.ProductSearchIndexDocument;
import com.tuzki.mall.admin.product.search.ProductSearchIndexDocumentBuilder;
import com.tuzki.mall.admin.product.search.ProductSearchIndexRequestFactory;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.search.config.ElasticsearchProperties;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ElasticsearchProductSearchIndexServiceTest {

    @Test
    void syncProductCreatesIndexIfNeededAndIndexesDocument() throws Exception {
        RestClient restClient = mock(RestClient.class);
        Response response = mock(Response.class);
        when(restClient.performRequest(any(Request.class))).thenReturn(response);
        ProductSearchIndexDocumentBuilder documentBuilder = mock(ProductSearchIndexDocumentBuilder.class);
        ProductSearchIndexRequestFactory requestFactory = mock(ProductSearchIndexRequestFactory.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setProductIndex("mall_product_search_v1");
        ProductSearchIndexDocument document = new ProductSearchIndexDocument();
        document.setProductId(100L);

        when(documentBuilder.build(100L)).thenReturn(Optional.of(document));
        when(requestFactory.buildIndexDocumentBody(document)).thenReturn("{\"productId\":100}");

        ElasticsearchProductSearchIndexService service = new ElasticsearchProductSearchIndexService(
                restClient, properties, documentBuilder, requestFactory, productMapper);

        service.syncProduct(100L);

        org.mockito.ArgumentCaptor<Request> requestCaptor = org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(restClient, times(2)).performRequest(requestCaptor.capture());
        assertEquals("HEAD", requestCaptor.getAllValues().get(0).getMethod());
        assertEquals("/mall_product_search_v1", requestCaptor.getAllValues().get(0).getEndpoint());
        assertEquals("PUT", requestCaptor.getAllValues().get(1).getMethod());
        assertEquals("/mall_product_search_v1/_doc/100", requestCaptor.getAllValues().get(1).getEndpoint());
    }

    @Test
    void syncProductDeletesDocumentWhenMysqlDocumentIsMissing() throws Exception {
        RestClient restClient = mock(RestClient.class);
        Response response = mock(Response.class);
        when(restClient.performRequest(any(Request.class))).thenReturn(response);
        ProductSearchIndexDocumentBuilder documentBuilder = mock(ProductSearchIndexDocumentBuilder.class);
        ProductSearchIndexRequestFactory requestFactory = mock(ProductSearchIndexRequestFactory.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setProductIndex("mall_product_search_v1");
        when(documentBuilder.build(100L)).thenReturn(Optional.empty());

        ElasticsearchProductSearchIndexService service = new ElasticsearchProductSearchIndexService(
                restClient, properties, documentBuilder, requestFactory, productMapper);

        service.syncProduct(100L);

        org.mockito.ArgumentCaptor<Request> requestCaptor = org.mockito.ArgumentCaptor.forClass(Request.class);
        verify(restClient).performRequest(requestCaptor.capture());
        assertEquals("DELETE", requestCaptor.getValue().getMethod());
        assertEquals("/mall_product_search_v1/_doc/100", requestCaptor.getValue().getEndpoint());
    }
}
