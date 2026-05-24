package com.tuzki.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.order.entity.OrderItem;

/**
 * 订单明细数据访问接口，负责订单 SKU 明细和商品快照信息的新增、查询。
 */
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
