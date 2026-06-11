package com.tuzki.mall.seckill.message;

/**
 * 秒杀下单消息发送接口，隔离秒杀业务模块和具体 RabbitMQ 客户端实现。
 */
public interface SeckillOrderMessageSender {

    /**
     * 发送秒杀异步下单消息。
     *
     * @param message 秒杀下单消息，包含请求流水 ID、用户 ID、秒杀商品 ID、请求幂等号、收货地址、购买数量和备注
     */
    void send(SeckillOrderMessage message);
}
