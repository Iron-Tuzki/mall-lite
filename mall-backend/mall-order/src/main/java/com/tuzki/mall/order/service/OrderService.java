package com.tuzki.mall.order.service;

import com.tuzki.mall.order.dto.OrderCreateRequest;
import com.tuzki.mall.order.vo.OrderCreateVO;
import com.tuzki.mall.order.vo.OrderDetailVO;
import com.tuzki.mall.order.vo.OrderMainVO;

import java.util.List;

/**
 * 订单业务接口，负责创建订单以及后续订单状态流转。
 */
public interface OrderService {

    /**
     * 创建订单并锁定订单中每个 SKU 对应的库存。
     *
     * @param userId 当前登录用户 ID，由登录态解析得到
     * @param request 创建订单请求，包含收货地址 ID、幂等请求号、订单明细列表和备注
     * @return 创建后的订单核心信息
     */
    OrderCreateVO createOrder(Long userId, OrderCreateRequest request);

    /**
     * 根据订单 ID 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单详情信息，包含订单主信息和订单明细
     */
    OrderDetailVO getOrderById(Long orderId);

    /**
     * 取消待支付订单，并释放订单中已锁定的所有 SKU 库存。
     *
     * @param orderId 订单 ID
     */
    void cancelOrder(Long orderId);

    /**
     * 查询指定用户的订单列表。
     *
     * @param userId 用户 ID
     * @return 订单主信息列表
     */
    List<OrderMainVO> listOrders(Long userId);
}
