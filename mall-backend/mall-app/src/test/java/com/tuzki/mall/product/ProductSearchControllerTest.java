package com.tuzki.mall.product;

import com.tuzki.mall.common.web.GlobalExceptionHandler;
import com.tuzki.mall.product.controller.ProductSearchController;
import com.tuzki.mall.product.search.dto.ProductSearchRequest;
import com.tuzki.mall.product.search.service.ProductSearchService;
import com.tuzki.mall.product.search.vo.ProductSearchAggVO;
import com.tuzki.mall.product.search.vo.ProductSearchItemVO;
import com.tuzki.mall.product.search.vo.ProductSearchResultVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductSearchController.class)
@Import(GlobalExceptionHandler.class)
class ProductSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchService productSearchService;

    @Test
    void searchBindsRequestParametersAndReturnsSearchResult() throws Exception {
        ProductSearchResultVO result = new ProductSearchResultVO();
        result.setPageNo(1);
        result.setPageSize(10);
        result.setTotal(1);
        ProductSearchItemVO item = new ProductSearchItemVO();
        item.setProductId(4001L);
        item.setProductName("三模机械键盘");
        item.setPrice(new BigDecimal("299.00"));
        result.setRecords(List.of(item));
        ProductSearchAggVO brandAgg = new ProductSearchAggVO();
        brandAgg.setKey("KeyMaster");
        brandAgg.setCount(1L);
        result.setBrandAggs(List.of(brandAgg));
        when(productSearchService.search(any(ProductSearchRequest.class))).thenReturn(result);

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "键盘")
                        .param("categoryId", "20")
                        .param("brandName", "KeyMaster")
                        .param("minPrice", "100")
                        .param("maxPrice", "800")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].productId").value(4001))
                .andExpect(jsonPath("$.data.brandAggs[0].key").value("KeyMaster"));

        ArgumentCaptor<ProductSearchRequest> requestCaptor = ArgumentCaptor.forClass(ProductSearchRequest.class);
        verify(productSearchService).search(requestCaptor.capture());
        ProductSearchRequest request = requestCaptor.getValue();
        assertEquals("键盘", request.getKeyword());
        assertEquals(20L, request.getCategoryId());
        assertEquals("KeyMaster", request.getBrandName());
        assertEquals(new BigDecimal("100"), request.getMinPrice());
        assertEquals(new BigDecimal("800"), request.getMaxPrice());
        assertEquals(1, request.getPageNo());
        assertEquals(10, request.getPageSize());
    }
}
