package com.tuzki.mall.seckill.controller;

import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀后台接口控制器，提供活动库存预热等管理能力。
 */
@RestController
@RequestMapping("/api/admin/seckill")
public class SeckillAdminController {

    private final SeckillService seckillService;

    public SeckillAdminController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/activities/{activityId}/preheat")
    public Result<Void> preheatActivity(@PathVariable Long activityId) {
        seckillService.preheatActivity(activityId);
        return Result.success();
    }
}
