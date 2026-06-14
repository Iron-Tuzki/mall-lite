package com.tuzki.mall.admin.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tuzki.mall.admin.seckill.dto.AdminSeckillActivityRequest;
import com.tuzki.mall.admin.seckill.dto.AdminSeckillSkuRequest;
import com.tuzki.mall.admin.seckill.service.AdminSeckillService;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillActivityVO;
import com.tuzki.mall.admin.seckill.vo.AdminSeckillSkuVO;
import com.tuzki.mall.common.api.PageResult;
import com.tuzki.mall.common.exception.BusinessException;
import com.tuzki.mall.product.entity.Product;
import com.tuzki.mall.product.entity.Sku;
import com.tuzki.mall.product.mapper.ProductMapper;
import com.tuzki.mall.product.mapper.SkuMapper;
import com.tuzki.mall.seckill.entity.SeckillActivity;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillActivityMapper;
import com.tuzki.mall.seckill.mapper.SeckillSkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 后台秒杀管理默认实现，负责秒杀活动和活动商品配置的校验与持久化。
 */
@Service
public class AdminSeckillServiceImpl implements AdminSeckillService {

    private static final int NOT_DELETED = 0;

    private static final int DELETED = 1;

    private static final int DEFAULT_PAGE_NO = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;

    private final SeckillActivityMapper seckillActivityMapper;

    private final SeckillSkuMapper seckillSkuMapper;

    private final SkuMapper skuMapper;

    private final ProductMapper productMapper;

    public AdminSeckillServiceImpl(SeckillActivityMapper seckillActivityMapper,
                                   SeckillSkuMapper seckillSkuMapper,
                                   SkuMapper skuMapper,
                                   ProductMapper productMapper) {
        this.seckillActivityMapper = seckillActivityMapper;
        this.seckillSkuMapper = seckillSkuMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
    }

    @Override
    public PageResult<AdminSeckillActivityVO> listActivities(Integer pageNo,
                                                             Integer pageSize,
                                                             String keyword,
                                                             Integer status) {
        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        long offset = (long) (safePageNo - 1) * safePageSize;
        long total = seckillActivityMapper.selectCount(activityQuery(keyword, status));
        List<AdminSeckillActivityVO> records = seckillActivityMapper.selectList(activityQuery(keyword, status)
                        .orderByDesc(SeckillActivity::getId)
                        .last("LIMIT " + safePageSize + " OFFSET " + offset))
                .stream()
                .map(activity -> toActivityVO(activity, false))
                .toList();
        return new PageResult<>(safePageNo, safePageSize, total, records);
    }

    @Override
    public AdminSeckillActivityVO getActivity(Long activityId) {
        return toActivityVO(getActivityOrThrow(activityId), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO createActivity(AdminSeckillActivityRequest request) {
        validateActivityTime(request);
        SeckillActivity activity = new SeckillActivity();
        fillActivity(activity, request);
        activity.setDeleted(NOT_DELETED);
        seckillActivityMapper.insert(activity);
        return getActivity(activity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO updateActivity(Long activityId, AdminSeckillActivityRequest request) {
        validateActivityTime(request);
        SeckillActivity activity = getActivityOrThrow(activityId);
        fillActivity(activity, request);
        seckillActivityMapper.updateById(activity);
        return getActivity(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteActivity(Long activityId) {
        SeckillActivity activity = getActivityOrThrow(activityId);
        activity.setDeleted(DELETED);
        seckillActivityMapper.updateById(activity);
        for (SeckillSku seckillSku : listActivitySkus(activityId)) {
            seckillSku.setDeleted(DELETED);
            seckillSkuMapper.updateById(seckillSku);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillActivityVO updateActivityStatus(Long activityId, Integer status) {
        SeckillActivity activity = getActivityOrThrow(activityId);
        activity.setStatus(status);
        seckillActivityMapper.updateById(activity);
        return getActivity(activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillSkuVO addSku(Long activityId, AdminSeckillSkuRequest request) {
        getActivityOrThrow(activityId);
        Sku sku = getSkuOrThrow(request.getSkuId());
        validateSeckillSkuRequest(request, sku);

        SeckillSku existing = seckillSkuMapper.selectOne(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId)
                .eq(SeckillSku::getSkuId, request.getSkuId()));
        if (existing != null && Integer.valueOf(NOT_DELETED).equals(existing.getDeleted())) {
            throw new BusinessException(400, "seckill sku already exists");
        }
        SeckillSku seckillSku = existing == null ? new SeckillSku() : existing;
        fillSeckillSku(seckillSku, activityId, request);
        seckillSku.setDeleted(NOT_DELETED);
        if (existing == null) {
            seckillSkuMapper.insert(seckillSku);
        } else {
            seckillSkuMapper.updateById(seckillSku);
        }
        return toSkuVO(seckillSku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminSeckillSkuVO updateSku(Long activityId, Long seckillSkuId, AdminSeckillSkuRequest request) {
        getActivityOrThrow(activityId);
        SeckillSku seckillSku = getSeckillSkuOrThrow(activityId, seckillSkuId);
        if (!seckillSku.getSkuId().equals(request.getSkuId())) {
            throw new BusinessException(400, "skuId can not be changed");
        }
        Sku sku = getSkuOrThrow(request.getSkuId());
        validateSeckillSkuRequest(request, sku);
        fillSeckillSku(seckillSku, activityId, request);
        seckillSkuMapper.updateById(seckillSku);
        return toSkuVO(seckillSku);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long activityId, Long seckillSkuId) {
        SeckillSku seckillSku = getSeckillSkuOrThrow(activityId, seckillSkuId);
        seckillSku.setDeleted(DELETED);
        seckillSkuMapper.updateById(seckillSku);
    }

    private LambdaQueryWrapper<SeckillActivity> activityQuery(String keyword, Integer status) {
        LambdaQueryWrapper<SeckillActivity> queryWrapper = new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getDeleted, NOT_DELETED);
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(SeckillActivity::getName, keyword);
        }
        if (status != null) {
            queryWrapper.eq(SeckillActivity::getStatus, status);
        }
        return queryWrapper;
    }

    private void validateActivityTime(AdminSeckillActivityRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(400, "end time must be after start time");
        }
    }

    private void validateSeckillSkuRequest(AdminSeckillSkuRequest request, Sku sku) {
        if (request.getLimitQuantity() > request.getStockCount()) {
            throw new BusinessException(400, "limit quantity must not exceed stock count");
        }
        if (request.getSeckillPrice().compareTo(sku.getPrice()) > 0) {
            throw new BusinessException(400, "seckill price must not exceed sku price");
        }
    }

    private void fillActivity(SeckillActivity activity, AdminSeckillActivityRequest request) {
        activity.setName(request.getName());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setStatus(request.getStatus());
        activity.setRemark(request.getRemark());
    }

    private void fillSeckillSku(SeckillSku seckillSku, Long activityId, AdminSeckillSkuRequest request) {
        seckillSku.setActivityId(activityId);
        seckillSku.setSkuId(request.getSkuId());
        seckillSku.setSeckillPrice(request.getSeckillPrice());
        seckillSku.setStockCount(request.getStockCount());
        seckillSku.setLimitQuantity(request.getLimitQuantity());
        seckillSku.setSort(request.getSort());
        seckillSku.setStatus(request.getStatus());
    }

    private SeckillActivity getActivityOrThrow(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectOne(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, activityId)
                .eq(SeckillActivity::getDeleted, NOT_DELETED));
        if (activity == null) {
            throw new BusinessException(404, "seckill activity not found");
        }
        return activity;
    }

    private SeckillSku getSeckillSkuOrThrow(Long activityId, Long seckillSkuId) {
        SeckillSku seckillSku = seckillSkuMapper.selectOne(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getId, seckillSkuId)
                .eq(SeckillSku::getActivityId, activityId)
                .eq(SeckillSku::getDeleted, NOT_DELETED));
        if (seckillSku == null) {
            throw new BusinessException(404, "seckill sku not found");
        }
        return seckillSku;
    }

    private Sku getSkuOrThrow(Long skuId) {
        Sku sku = skuMapper.selectOne(new LambdaQueryWrapper<Sku>()
                .eq(Sku::getId, skuId)
                .eq(Sku::getDeleted, NOT_DELETED));
        if (sku == null) {
            throw new BusinessException(404, "sku not found");
        }
        return sku;
    }

    private List<SeckillSku> listActivitySkus(Long activityId) {
        return seckillSkuMapper.selectList(new LambdaQueryWrapper<SeckillSku>()
                .eq(SeckillSku::getActivityId, activityId)
                .eq(SeckillSku::getDeleted, NOT_DELETED)
                .orderByAsc(SeckillSku::getSort)
                .orderByDesc(SeckillSku::getId));
    }

    private AdminSeckillActivityVO toActivityVO(SeckillActivity activity, boolean includeSkus) {
        AdminSeckillActivityVO activityVO = new AdminSeckillActivityVO();
        activityVO.setId(activity.getId());
        activityVO.setName(activity.getName());
        activityVO.setStartTime(activity.getStartTime());
        activityVO.setEndTime(activity.getEndTime());
        activityVO.setStatus(activity.getStatus());
        activityVO.setRemark(activity.getRemark());
        if (includeSkus) {
            activityVO.setSkus(listActivitySkus(activity.getId()).stream()
                    .map(this::toSkuVO)
                    .toList());
        } else {
            activityVO.setSkus(List.of());
        }
        return activityVO;
    }

    private AdminSeckillSkuVO toSkuVO(SeckillSku seckillSku) {
        Sku sku = skuMapper.selectById(seckillSku.getSkuId());
        Product product = sku == null ? null : productMapper.selectById(sku.getProductId());
        AdminSeckillSkuVO skuVO = new AdminSeckillSkuVO();
        skuVO.setId(seckillSku.getId());
        skuVO.setActivityId(seckillSku.getActivityId());
        skuVO.setSkuId(seckillSku.getSkuId());
        skuVO.setProductName(product == null ? null : product.getName());
        skuVO.setSkuName(sku == null ? null : sku.getSkuName());
        skuVO.setOriginalPrice(sku == null ? null : sku.getPrice());
        skuVO.setSeckillPrice(seckillSku.getSeckillPrice());
        skuVO.setStockCount(seckillSku.getStockCount());
        skuVO.setLimitQuantity(seckillSku.getLimitQuantity());
        skuVO.setSort(seckillSku.getSort());
        skuVO.setStatus(seckillSku.getStatus());
        return skuVO;
    }

    private int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo < DEFAULT_PAGE_NO) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
