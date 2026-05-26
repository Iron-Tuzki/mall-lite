USE mall_lite;

ALTER TABLE pms_payment
    ADD COLUMN pending_order_id BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN status = 10 AND deleted = 0 THEN order_id ELSE NULL END
    ) STORED COMMENT '待支付订单ID生成列，用于限制同一订单同一渠道只有一条待支付流水' AFTER status;

ALTER TABLE pms_payment
    ADD UNIQUE KEY uk_pending_order_channel (pending_order_id, pay_channel);
