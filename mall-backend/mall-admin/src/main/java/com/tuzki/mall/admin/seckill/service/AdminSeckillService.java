package com.tuzki.mall.admin.seckill.service;

import com.tuzki.mall.admin.seckill.dto.AdminSeckillActivityRequest;
import com.tuzki.mall.admin.seckill.dto.AdminSeckillSkuRequest;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillActivityVO;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillSkuVO;
import com.tuzki.mall.common.api.PageResult;

/**
 * 后台秒杀管理接口，负责秒杀活动和活动商品配置维护。
 */
public interface AdminSeckillService {

    /**
     * 分页查询秒杀活动。
     *
     * @param pageNo 当前页码
     * @param pageSize 每页数量
     * @param keyword 活动名称关键字
     * @param status 活动状态
     * @return 秒杀活动分页结果
     */
    PageResult<AdminSeckillActivityVO> listActivities(Integer pageNo, Integer pageSize, String keyword, Integer status);

    /**
     * 查询秒杀活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    AdminSeckillActivityVO getActivity(Long activityId);

    /**
     * 创建秒杀活动。
     *
     * @param request 活动创建请求
     * @return 创建后的活动详情
     */
    AdminSeckillActivityVO createActivity(AdminSeckillActivityRequest request);

    /**
     * 更新秒杀活动。
     *
     * @param activityId 活动 ID
     * @param request 活动更新请求
     * @return 更新后的活动详情
     */
    AdminSeckillActivityVO updateActivity(Long activityId, AdminSeckillActivityRequest request);

    /**
     * 软删除秒杀活动。
     *
     * @param activityId 活动 ID
     */
    void deleteActivity(Long activityId);

    /**
     * 更新秒杀活动状态。
     *
     * @param activityId 活动 ID
     * @param status 活动状态
     * @return 更新后的活动详情
     */
    AdminSeckillActivityVO updateActivityStatus(Long activityId, Integer status);

    /**
     * 添加活动商品。
     *
     * @param activityId 活动 ID
     * @param request 活动商品请求
     * @return 活动商品详情
     */
    AdminSeckillSkuVO addSku(Long activityId, AdminSeckillSkuRequest request);

    /**
     * 更新活动商品。
     *
     * @param activityId 活动 ID
     * @param seckillSkuId 活动商品 ID
     * @param request 活动商品请求
     * @return 活动商品详情
     */
    AdminSeckillSkuVO updateSku(Long activityId, Long seckillSkuId, AdminSeckillSkuRequest request);

    /**
     * 软删除活动商品。
     *
     * @param activityId 活动 ID
     * @param seckillSkuId 活动商品 ID
     */
    void deleteSku(Long activityId, Long seckillSkuId);
}
