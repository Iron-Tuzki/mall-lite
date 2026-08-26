package com.tuzki.mall.admin;

import com.tuzki.mall.admin.controller.AdminProductSearchIndexController;
import com.tuzki.mall.admin.product.search.ProductSearchIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductSearchIndexController.class)
class AdminProductSearchIndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductSearchIndexService productSearchIndexService;

    @Test
    void rebuildProductSearchIndexCallsIndexService() throws Exception {
        mockMvc.perform(post("/api/admin/products/search-index/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productSearchIndexService).rebuildAll();
    }

    @Test
    void syncSingleProductCallsIndexService() throws Exception {
        mockMvc.perform(post("/api/admin/products/search-index/100/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(productSearchIndexService).syncProduct(100L);
    }
}
