package com.tuzki.mall.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tuzki.mall.payment.entity.Payment;

/**
 * 支付流水数据访问接口，负责支付流水记录的新增、查询和后续支付状态更新。
 */
public interface PaymentMapper extends BaseMapper<Payment> {
}
