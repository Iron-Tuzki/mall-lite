package com.tuzki.mall.product.mq;

import com.tuzki.mall.product.hot.ProductHotEvent;
import com.tuzki.mall.product.service.ProductHotService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 商品热点事件消费者，负责将热点事件写入 Redis 小时榜单。
 */
@Component
public class ProductHotConsumer {

    private final ProductHotService productHotService;

    public ProductHotConsumer(ProductHotService productHotService) {
        this.productHotService = productHotService;
    }

    /**
     * 消费商品热点事件消息。
     *
     * @param event 商品热点事件
     */
    @RabbitListener(queues = "${mall.product.hot-rabbit.event-queue}")
    public void handle(ProductHotEvent event) {
        productHotService.handleEvent(event);
    }
}
