package com.tuzki.mall.seckill.service;

import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.seckill.dto.SeckillOrderCreateRequest;
import com.tuzki.mall.seckill.vo.SeckillActivityVO;

import java.util.List;

/**
 * 秒杀业务接口，负责查询当前活动、预热活动库存以及创建秒杀订单。
 */
public interface SeckillService {

    /**
     * 查询当前时间可参与的秒杀活动。
     *
     * @return 当前有效秒杀活动列表
     */
    List<SeckillActivityVO> listActiveActivities();

    /**
     * 预热指定活动下的秒杀商品库存到 Redis。
     *
     * @param activityId 秒杀活动 ID
     */
    void preheatActivity(Long activityId);

    /**
     * 创建秒杀订单。
     *
     * @param userId 当前登录用户 ID
     * @param request 秒杀下单请求
     * @return 创建后的订单核心信息
     */
    OrderCreateVO createSeckillOrder(Long userId, SeckillOrderCreateRequest request);
}
