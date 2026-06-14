package com.tuzki.mall.admin.controller;

import com.tuzki.mall.admin.product.dto.AdminStatusUpdateRequest;
import com.tuzki.mall.admin.seckill.dto.AdminSeckillActivityRequest;
import com.tuzki.mall.admin.seckill.dto.AdminSeckillSkuRequest;
import com.tuzki.mall.admin.seckill.service.AdminSeckillService;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillActivityVO;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillSkuVO;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.api.Result;
import com.tuzki.mall.seckill.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台秒杀活动管理控制器，提供活动和活动商品配置维护接口。
 */
@RestController
@RequestMapping("/api/admin/seckill/activities")
public class AdminSeckillActivityController {

    private final AdminSeckillService adminSeckillService;

    private final SeckillService seckillService;

    public AdminSeckillActivityController(AdminSeckillService adminSeckillService,
                                          SeckillService seckillService) {
        this.adminSeckillService = adminSeckillService;
        this.seckillService = seckillService;
    }

    @GetMapping
    public Result<PageResult<AdminSeckillActivityVO>> listActivities(@RequestParam(required = false) Integer pageNo,
                                                                     @RequestParam(required = false) Integer pageSize,
                                                                     @RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) Integer status) {
        return Result.success(adminSeckillService.listActivities(pageNo, pageSize, keyword, status));
    }

    @PostMapping
    public Result<AdminSeckillActivityVO> createActivity(@Valid @RequestBody AdminSeckillActivityRequest request) {
        return Result.success(adminSeckillService.createActivity(request));
    }

    @GetMapping("/{activityId}")
    public Result<AdminSeckillActivityVO> getActivity(@PathVariable Long activityId) {
        return Result.success(adminSeckillService.getActivity(activityId));
    }

    @PutMapping("/{activityId}")
    public Result<AdminSeckillActivityVO> updateActivity(@PathVariable Long activityId,
                                                         @Valid @RequestBody AdminSeckillActivityRequest request) {
        return Result.success(adminSeckillService.updateActivity(activityId, request));
    }

    @DeleteMapping("/{activityId}")
    public Result<Void> deleteActivity(@PathVariable Long activityId) {
        adminSeckillService.deleteActivity(activityId);
        return Result.success();
    }

    @PutMapping("/{activityId}/status")
    public Result<AdminSeckillActivityVO> updateStatus(@PathVariable Long activityId,
                                                       @Valid @RequestBody AdminStatusUpdateRequest request) {
        return Result.success(adminSeckillService.updateActivityStatus(activityId, request.getStatus()));
    }

    @PostMapping("/{activityId}/preheat")
    public Result<Void> preheatActivity(@PathVariable Long activityId) {
        seckillService.preheatActivity(activityId);
        return Result.success();
    }

    @PostMapping("/{activityId}/skus")
    public Result<AdminSeckillSkuVO> addSku(@PathVariable Long activityId,
                                            @Valid @RequestBody AdminSeckillSkuRequest request) {
        return Result.success(adminSeckillService.addSku(activityId, request));
    }

    @PutMapping("/{activityId}/skus/{seckillSkuId}")
    public Result<AdminSeckillSkuVO> updateSku(@PathVariable Long activityId,
                                               @PathVariable Long seckillSkuId,
                                               @Valid @RequestBody AdminSeckillSkuRequest request) {
        return Result.success(adminSeckillService.updateSku(activityId, seckillSkuId, request));
    }

    @DeleteMapping("/{activityId}/skus/{seckillSkuId}")
    public Result<Void> deleteSku(@PathVariable Long activityId,
                                  @PathVariable Long seckillSkuId) {
        adminSeckillService.deleteSku(activityId, seckillSkuId);
        return Result.success();
    }
}
