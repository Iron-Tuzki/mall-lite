USE mall_lite;

ALTER TABLE oms_order
    ADD COLUMN cancel_type TINYINT UNSIGNED NULL COMMENT '取消类型：10用户主动取消，20超时自动取消' AFTER status,
    ADD COLUMN cancel_reason VARCHAR(100) NULL COMMENT '取消原因' AFTER cancel_type;
