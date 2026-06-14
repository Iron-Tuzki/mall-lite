package com.tuzki.mall.admin;

import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@AutoConfigureMockMvc
@Transactional
class AdminSeckillApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Test
    void createSeckillActivityPersistsActivity() throws Exception {
        long beforeCount = seckillActivityMapper.selectCount(null);
        long suffix = System.nanoTime();

        mockMvc.perform(post("/api/admin/seckill/activities")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Admin Seckill %d",
                                  "startTime": "2026-06-14T12:00:00",
                                  "endTime": "2026-06-14T14:00:00",
                                  "status": 1,
                                  "remark": "created by admin test"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Admin Seckill " + suffix))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.skus.length()").value(0));

        assertEquals(beforeCount + 1, seckillActivityMapper.selectCount(null));
    }
}
