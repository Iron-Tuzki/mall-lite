package com.tuzki.mall.seckill.service;

import com.tuzki.mall.seckill.dto.SeckillOrderCreateRequest;
import com.tuzki.mall.seckill.message.SeckillOrderMessage;
import com.tuzki.mall.seckill.vo.SeckillActivityVO;
import com.tuzki.mall.seckill.vo.SeckillOrderResultVO;

import java.time.Duration;
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
     * 查询需要提前预热到 Redis 的秒杀活动 ID。
     *
     * @param preheatWindow 距离当前时间的预热窗口，窗口内即将开始且未结束的活动会被纳入预热范围
     * @return 需要预热的秒杀活动 ID 列表
     */
    List<Long> listPreheatableActivityIds(Duration preheatWindow);

    /**
     * 创建秒杀订单。
     *
     * @param userId 当前登录用户 ID
     * @param request 秒杀下单请求
     * @return 创建后的订单核心信息
     */
    SeckillOrderResultVO createSeckillOrder(Long userId, SeckillOrderCreateRequest request);

    /**
     * 查询秒杀下单结果。
     *
     * @param userId 当前登录用户 ID
     * @param seckillSkuId 秒杀活动商品 ID
     * @param requestId 秒杀请求幂等号
     * @return 秒杀请求当前结果，可能为 PROCESSING、SUCCESS 或 FAILED
     */
    SeckillOrderResultVO getSeckillOrderResult(Long userId, Long seckillSkuId, String requestId);

    /**
     * 处理 Redis 预扣成功后的秒杀异步下单消息。
     *
     * @param message 秒杀下单消息，包含创建订单所需的请求流水和下单参数
     */
    void processQueuedSeckillOrder(SeckillOrderMessage message);
}
