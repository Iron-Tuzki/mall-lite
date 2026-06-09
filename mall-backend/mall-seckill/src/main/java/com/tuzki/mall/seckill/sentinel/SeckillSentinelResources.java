package com.tuzki.mall.seckill.sentinel;

/**
 * 秒杀 Sentinel 资源名常量，统一维护秒杀相关限流、降级和熔断资源。
 */
public final class SeckillSentinelResources {

    public static final String SECKILL_CREATE_ORDER = "seckill-create-order";

    private SeckillSentinelResources() {
    }
}
