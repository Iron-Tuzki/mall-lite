package com.tuzki.mall.seckill.service;

import com.tuzki.mall.seckill.entity.SeckillRequest;
import com.tuzki.mall.seckill.entity.SeckillSku;
import com.tuzki.mall.seckill.mapper.SeckillRequestMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀请求流水服务，使用独立事务持久化请求状态，避免主下单事务回滚导致失败和补偿记录丢失。
 */
@Service
public class SeckillRequestLogService {

    private final SeckillRequestMapper seckillRequestMapper;

    public SeckillRequestLogService(SeckillRequestMapper seckillRequestMapper) {
        this.seckillRequestMapper = seckillRequestMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AcquireResult acquire(Long userId, SeckillSku seckillSku, String requestId, Integer quantity) {
        SeckillRequest seckillRequest = new SeckillRequest();
        seckillRequest.setRequestId(requestId);
        seckillRequest.setUserId(userId);
        seckillRequest.setActivityId(seckillSku.getActivityId());
        seckillRequest.setSeckillSkuId(seckillSku.getId());
        seckillRequest.setSkuId(seckillSku.getSkuId());
        seckillRequest.setQuantity(quantity);
        seckillRequest.setStatus(SeckillRequest.STATUS_INIT);
        seckillRequest.setRetryCount(0);
        seckillRequest.setDeleted(0);
        int insertedRows = seckillRequestMapper.insertIgnore(seckillRequest);
        if (insertedRows == 1) {
            return new AcquireResult(seckillRequest, true);
        }
        return new AcquireResult(selectByUniqueKey(userId, seckillSku.getId(), requestId), false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPreDeducted(Long id) {
        seckillRequestMapper.markPreDeducted(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOrderCreated(Long id, Long orderId) {
        seckillRequestMapper.markOrderCreated(id, orderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String failReason) {
        seckillRequestMapper.markFailed(id, normalizeReason(failReason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(Long id, String failReason) {
        seckillRequestMapper.markCompensated(id, normalizeReason(failReason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SeckillRequest getByUniqueKey(Long userId, Long seckillSkuId, String requestId) {
        return seckillRequestMapper.selectByUniqueKey(userId, seckillSkuId, requestId);
    }

    private SeckillRequest selectByUniqueKey(Long userId, Long seckillSkuId, String requestId) {
        return seckillRequestMapper.selectByUniqueKeyForUpdate(userId, seckillSkuId, requestId);
    }

    private String normalizeReason(String failReason) {
        if (failReason == null || failReason.isBlank()) {
            return "unknown error";
        }
        return failReason.length() <= 255 ? failReason : failReason.substring(0, 255);
    }

    /**
     * 秒杀请求流水抢占结果，包含当前流水和本次请求是否成功插入新流水。
     */
    public record AcquireResult(SeckillRequest request, boolean created) {
    }
}
