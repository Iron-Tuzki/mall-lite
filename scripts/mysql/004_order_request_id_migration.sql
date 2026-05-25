USE mall_lite;

ALTER TABLE oms_order
    ADD COLUMN request_id VARCHAR(64) NULL COMMENT '下单请求幂等号' AFTER order_no;

ALTER TABLE oms_order
    ADD UNIQUE KEY uk_user_request (user_id, request_id);
