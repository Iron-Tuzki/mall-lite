package com.tuzki.mall.product.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductSearchResultMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapSearchResponseConvertsHitsAndAggregationsToResultVo() throws Exception {
        String responseBody = """
                {
                  "hits": {
                    "total": {
                      "value": 2
                    },
                    "hits": [
                      {
                        "highlight": {
                          "productName": [
                            "<em>三模</em>机械键盘"
                          ],
                          "description": [
                            "支持蓝牙、有线和 2.4G 的<em>机械键盘</em>"
                          ]
                        },
                        "_source": {
                          "productId": 4001,
                          "productName": "三模机械键盘",
                          "categoryId": 20,
                          "categoryName": "电脑外设",
                          "brandName": "KeyMaster",
                          "price": 299.00,
                          "sales": 860,
                          "stock": 120,
                          "mainImageUrl": "https://example.test/keyboard.png"
                        }
                      }
                    ]
                  },
                  "aggregations": {
                    "by_brand": {
                      "buckets": [
                        {
                          "key": "KeyMaster",
                          "doc_count": 2
                        }
                      ]
                    },
                    "by_category": {
                      "buckets": [
                        {
                          "key": "电脑外设",
                          "doc_count": 2
                        }
                      ]
                    },
                    "price_ranges": {
                      "buckets": [
                        {
                          "key": "100-500 元",
                          "doc_count": 2
                        }
                      ]
                    }
                  }
                }
                """;

        ProductSearchResultVO result = new ProductSearchResultMapper(objectMapper).map(responseBody, 1, 10);

        assertEquals(1, result.getPageNo());
        assertEquals(10, result.getPageSize());
        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(4001L, result.getRecords().get(0).getProductId());
        assertEquals("三模机械键盘", result.getRecords().get(0).getProductName());
        assertEquals("<em>三模</em>机械键盘",
                result.getRecords().get(0).getHighlights().get("productName").get(0));
        assertEquals("支持蓝牙、有线和 2.4G 的<em>机械键盘</em>",
                result.getRecords().get(0).getHighlights().get("description").get(0));
        assertEquals(new BigDecimal("299.0"), result.getRecords().get(0).getPrice());
        assertEquals("KeyMaster", result.getBrandAggs().get(0).getKey());
        assertEquals(2L, result.getBrandAggs().get(0).getCount());
        assertEquals("电脑外设", result.getCategoryAggs().get(0).getKey());
        assertEquals("100-500 元", result.getPriceAggs().get(0).getKey());
    }
}
