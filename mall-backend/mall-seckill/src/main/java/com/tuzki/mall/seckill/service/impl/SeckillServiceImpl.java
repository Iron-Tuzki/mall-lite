package com.tuzki.mall.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.order.dto.OrderCreateItemRequest;
import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.service.OrderService;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.seckill.dto.SeckillOrderCreateRequest;
import com.tuzki.mall.seckill.entity.SeckillActivity;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import com.tuzki.mall.seckill.mapper.SeckillSkuMapper;
import com.tuzki.mall.seckill.redis.SeckillRedisService;
import com.tuzki.mall.seckill.service.SeckillService;
import com.tuzki.mall.seckill.vo.SeckillActivityVO;
import com.tuzki.mall.seckill.vo.SeckillSkuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 秒杀业务默认实现，编排活动校验、Redis 活动库存预扣和订单创建。
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final int ACTIVE_STATUS = 1;

    private static final int NOT_DELETED = 0;

    private static final long PREHEAT_TTL_EXTRA_MINUTES = 5L;

    private final SeckillActivityMapper seckillActivityMapper;

    private final SeckillSkuMapper seckillSkuMapper;

    private final ProductMapper productMapper;

    private final SkuMapper skuMapper;

    private final SeckillRedisService seckillRedisService;

    private final OrderService orderService;

    public SeckillServiceImpl(SeckillActivityMapper seckillActivityMapper,
                              SeckillSkuMapper seckillSkuMapper,
                              ProductMapper productMapper,
                              SkuMapper skuMapper,
                              SeckillRedisService seckillRedisService,
                              OrderService orderService) {
        this.seckillActivityMapper = seckillActivityMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.seckillRedisService = seckillRedisService;
        this.orderService = orderService;
    }

    @Override
    public List<SeckillActivityVO> listActiveActivities() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, ACTIVE_STATUS)
                .eq(SeckillActivity::getDeleted, NOT_DELETED)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime));
        return activities.stream()
                .map(this::toActivityVO)
                .toList();
    }

    @Override
    public void preheatActivity(Long activityId) {
        SeckillActivity activity = getActiveActivity(activityId);
        Duration ttl = ttlUntilActivityEnd(activity);
        List<SeckillSku> seckillSkus = listActiveSeckillSkus(activity.getId());
        for (SeckillSku seckillSku : seckillSkus) {
            seckillRedisService.preheatStock(seckillSku.getId(), seckillSku.getStockCount(), ttl);
        }
    }

    @Override
    public List<Long> listPreheatableActivityIds(Duration preheatWindow) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime preheatBefore = now.plus(normalizePreheatWindow(preheatWindow));
        return seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                        .select(SeckillActivity::getId)
                        .eq(SeckillActivity::getStatus, ACTIVE_STATUS)
                        .eq(SeckillActivity::getDeleted, NOT_DELETED)
                        .le(SeckillActivity::getStartTime, preheatBefore)
                        .gt(SeckillActivity::getEndTime, now)
                        .orderByAsc(SeckillActivity::getStartTime))
                .stream()
                .map(SeckillActivity::getId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createSeckillOrder(Long userId, SeckillOrderCreateRequest request) {
        SeckillSku seckillSku = getActiveSeckillSku(request.getSeckillSkuId());
        SeckillActivity activity = getActiveActivity(seckillSku.getActivityId());
        validateActivityTime(activity);
        validateQuantity(request, seckillSku);

        Duration ttl = ttlUntilActivityEnd(activity);
        // redis中预扣库存
        long preDeductResult = seckillRedisService.preDeduct(
                seckillSku.getId(),
                userId,
                request.getRequestId(),
                request.getQuantity(),
                seckillSku.getLimitQuantity(),
                ttl);
        if (preDeductResult != SeckillRedisService.PRE_DEDUCT_SUCCESS
                && preDeductResult != SeckillRedisService.DUPLICATED_REQUEST) {
            throwPreDeductException(preDeductResult);
        }

        OrderCreateRequest orderRequest = buildOrderCreateRequest(request, seckillSku);
        try {
            return orderService.createOrderWithPriceOverrides(
                    userId,
                    orderRequest,
                    Map.of(seckillSku.getSkuId(), seckillSku.getSeckillPrice()));
        } catch (RuntimeException exception) {
            // 如果发生了异常，并且redis成功扣减，则补偿回redis，相当于回滚redis
            if (preDeductResult == SeckillRedisService.PRE_DEDUCT_SUCCESS) {
                seckillRedisService.compensate(seckillSku.getId(), userId, request.getRequestId(), request.getQuantity());
            }
            throw exception;
        }
    }

    private SeckillActivityVO toActivityVO(SeckillActivity activity) {
        SeckillActivityVO activityVO = new SeckillActivityVO();
        activityVO.setId(activity.getId());
        activityVO.setName(activity.getName());
        activityVO.setStartTime(activity.getStartTime());
        activityVO.setEndTime(activity.getEndTime());
        activityVO.setStatus(activity.getStatus());
        activityVO.setSkus(listActiveSeckillSkus(activity.getId()).stream()
                .map(this::toSkuVO)
                .filter(Objects::nonNull)
                .toList());
        return activityVO;
    }

    private SeckillSkuVO toSkuVO(SeckillSku seckillSku) {
        Sku sku = getActiveSkuOrNull(seckillSku.getSkuId());
        if (sku == null) {
            return null;
        }
        Product product = getActiveProductOrNull(sku.getProductId());
        if (product == null) {
            return null;
        }

        SeckillSkuVO skuVO = new SeckillSkuVO();
        skuVO.setId(seckillSku.getId());
        skuVO.setActivityId(seckillSku.getActivityId());
        skuVO.setSkuId(seckillSku.getSkuId());
        skuVO.setProductName(product.getName());
        skuVO.setSkuName(sku.getSkuName());
        skuVO.setMainImageUrl(resolveMainImageUrl(sku, product));
        skuVO.setSeckillPrice(seckillSku.getSeckillPrice());
        skuVO.setStockCount(seckillSku.getStockCount());
        skuVO.setLimitQuantity(seckillSku.getLimitQuantity());
        skuVO.setStatus(seckillSku.getStatus());
        return skuVO;
    }

    private SeckillActivity getActiveActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectOne(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, activityId)
                .eq(SeckillActivity::getStatus, ACTIVE_STATUS)
                .eq(SeckillActivity::getDeleted, NOT_DELETED));
        if (activity == null) {
            throw new BusinessException(404, "seckill activity not found");
        }
        return activity;
    }

    private SeckillSku getActiveSeckillSku(Long seckillSkuId) {
        SeckillSku seckillSku = seckillSkuMapper.selectOne(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getId, seckillSkuId)
                .eq(SeckillSku::getStatus, ACTIVE_STATUS)
                .eq(SeckillSku::getDeleted, NOT_DELETED));
        if (seckillSku == null) {
            throw new BusinessException(404, "seckill sku not found");
        }
        return seckillSku;
    }

    private List<SeckillSku> listActiveSeckillSkus(Long activityId) {
        return seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId)
                .eq(SeckillSku::getStatus, ACTIVE_STATUS)
                .eq(SeckillSku::getDeleted, NOT_DELETED)
                .orderByAsc(SeckillSku::getSort)
                .orderByDesc(SeckillSku::getId));
    }

    private Sku getActiveSkuOrNull(Long skuId) {
        return skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .eq(Sku::getStatus, ACTIVE_STATUS)
                .eq(Sku::getDeleted, NOT_DELETED));
    }

    private Product getActiveProductOrNull(Long productId) {
        return productMapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ACTIVE_STATUS)
                .eq(Product::getDeleted, NOT_DELETED));
    }

    private void validateActivityTime(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(400, "seckill activity not started");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(400, "seckill activity ended");
        }
    }

    private void validateQuantity(SeckillOrderCreateRequest request, SeckillSku seckillSku) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BusinessException(400, "quantity must be greater than 0");
        }
        if (request.getQuantity() > seckillSku.getLimitQuantity()) {
            throw new BusinessException(400, "seckill purchase limit exceeded");
        }
    }

    private void throwPreDeductException(long result) {
        if (result == SeckillRedisService.PURCHASE_LIMIT_EXCEEDED) {
            throw new BusinessException(400, "seckill purchase limit exceeded");
        }
        if (result == SeckillRedisService.STOCK_SOLD_OUT) {
            throw new BusinessException(400, "seckill stock sold out");
        }
        if (result == SeckillRedisService.STOCK_NOT_PREHEATED) {
            throw new BusinessException(400, "seckill stock not preheated");
        }
        throw new BusinessException(400, "seckill stock deduct failed");
    }

    private OrderCreateRequest buildOrderCreateRequest(SeckillOrderCreateRequest request, SeckillSku seckillSku) {
        OrderCreateItemRequest itemRequest = new OrderCreateItemRequest();
        itemRequest.setSkuId(seckillSku.getSkuId());
        itemRequest.setQuantity(request.getQuantity());

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setRequestId("seckill:" + seckillSku.getId() + ":" + request.getRequestId());
        orderRequest.setAddressId(request.getAddressId());
        orderRequest.setItems(List.of(itemRequest));
        orderRequest.setRemark(request.getRemark());
        return orderRequest;
    }

    private Duration ttlUntilActivityEnd(SeckillActivity activity) {
        // 到期时间比活动时间多五分钟
        return Duration.between(LocalDateTime.now(), activity.getEndTime())
                .plusMinutes(PREHEAT_TTL_EXTRA_MINUTES);
    }

    private Duration normalizePreheatWindow(Duration preheatWindow) {
        if (preheatWindow == null || preheatWindow.isNegative()) {
            return Duration.ZERO;
        }
        return preheatWindow;
    }

    private String resolveMainImageUrl(Sku sku, Product product) {
        if (sku.getMainImageUrl() != null && !sku.getMainImageUrl().isBlank()) {
            return sku.getMainImageUrl();
        }
        return product.getMainImageUrl();
    }
}
