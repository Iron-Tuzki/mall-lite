package com.tuzki.mall.seckill.listener;

import com.tuzki.mall.order.event.OrderCancelledEvent;
import com.tuzki.mall.seckill.service.SeckillCompensationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单取消监听器，负责在普通订单取消成功后触发秒杀库存的幂等补偿。
 */
@Component
public class SeckillOrderCancelledListener {

    private final SeckillCompensationService seckillCompensationService;

    public SeckillOrderCancelledListener(SeckillCompensationService seckillCompensationService) {
        this.seckillCompensationService = seckillCompensationService;
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        seckillCompensationService.compensateCancelledOrder(event.orderId());
    }
}
