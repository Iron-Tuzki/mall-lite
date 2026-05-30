USE mall_lite;

CREATE TABLE IF NOT EXISTS sms_coupon_template
(
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '优惠券模板ID',
    coupon_name      VARCHAR(100)     NOT NULL COMMENT '优惠券名称',
    coupon_type      TINYINT UNSIGNED NOT NULL COMMENT '优惠券类型：1满减券，2折扣券',
    threshold_amount DECIMAL(10, 2)   NULL COMMENT '使用门槛金额',
    discount_amount  DECIMAL(10, 2)   NULL COMMENT '优惠金额',
    discount_rate    DECIMAL(5, 2)    NULL COMMENT '折扣率，例如8.50表示85折',
    validity_type    TINYINT UNSIGNED NOT NULL COMMENT '有效期类型：1固定时间，2领取后N天',
    valid_start_time DATETIME         NULL COMMENT '固定有效期开始时间',
    valid_end_time   DATETIME         NULL COMMENT '固定有效期结束时间',
    valid_days       INT UNSIGNED     NULL COMMENT '领取后有效天数',
    total_stock      INT UNSIGNED     NULL COMMENT '总库存，NULL表示不限量',
    received_count   INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '已领取数量',
    status           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    create_time      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_status (status, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '优惠券模板表';

CREATE TABLE IF NOT EXISTS sms_user_coupon
(
    id                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '用户优惠券ID',
    user_id            BIGINT UNSIGNED  NOT NULL COMMENT '用户ID',
    coupon_template_id BIGINT UNSIGNED  NOT NULL COMMENT '优惠券模板ID',
    coupon_name        VARCHAR(100)     NOT NULL COMMENT '优惠券名称快照',
    coupon_type        TINYINT UNSIGNED NOT NULL COMMENT '优惠券类型快照：1满减券，2折扣券',
    threshold_amount   DECIMAL(10, 2)   NULL COMMENT '使用门槛金额快照',
    discount_amount    DECIMAL(10, 2)   NULL COMMENT '优惠金额快照',
    discount_rate      DECIMAL(5, 2)    NULL COMMENT '折扣率快照',
    valid_start_time   DATETIME         NOT NULL COMMENT '有效期开始时间',
    valid_end_time     DATETIME         NOT NULL COMMENT '有效期结束时间',
    status             TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1未使用，2已使用，3已过期',
    order_id           BIGINT UNSIGNED  NULL COMMENT '使用该优惠券的订单ID',
    used_time          DATETIME         NULL COMMENT '使用时间',
    source_type        TINYINT UNSIGNED NOT NULL COMMENT '来源类型：1签到奖励，2活动领取，3后台发放',
    source_key         VARCHAR(100)     NOT NULL COMMENT '来源业务唯一键，例如SIGN_IN_7_202605',
    create_time        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted            TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_source (user_id, source_type, source_key),
    KEY idx_user_status (user_id, status, deleted),
    KEY idx_valid_end_time (valid_end_time),
    KEY idx_coupon_template_id (coupon_template_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '用户优惠券表';

INSERT INTO sms_coupon_template
(id, coupon_name, coupon_type, threshold_amount, discount_amount, discount_rate, validity_type, valid_days, total_stock, status, deleted)
VALUES (700001, '连续签到7天满100减10券', 1, 100.00, 10.00, NULL, 2, 30, NULL, 1, 0),
       (700002, '连续签到15天满200减25券', 1, 200.00, 25.00, NULL, 2, 30, NULL, 1, 0)
ON DUPLICATE KEY UPDATE coupon_name      = VALUES(coupon_name),
                        coupon_type      = VALUES(coupon_type),
                        threshold_amount = VALUES(threshold_amount),
                        discount_amount  = VALUES(discount_amount),
                        discount_rate    = VALUES(discount_rate),
                        validity_type    = VALUES(validity_type),
                        valid_days       = VALUES(valid_days),
                        total_stock      = VALUES(total_stock),
                        status           = VALUES(status),
                        deleted          = VALUES(deleted);
