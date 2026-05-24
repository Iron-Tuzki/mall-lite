package com.tuzki.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.order.entity.Order;

/**
 * 订单主表数据访问接口，负责订单主记录的新增、查询和后续状态更新。
 */
public interface OrderMapper extends BaseMapper<Order> {
}
