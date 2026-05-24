package com.tuzki.mall.order.service;

import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.vo.OrderCreateVO;

/**
 * 订单业务接口，负责创建订单以及后续订单状态流转。
 */
public interface OrderService {

    /**
     * 创建订单并锁定对应 SKU 库存。
     *
     * @param request 创建订单请求，包含用户 ID、收货地址 ID、SKU ID、购买数量和备注
     * @return 创建后的订单核心信息
     */
    OrderCreateVO createOrder(OrderCreateRequest request);
}
