USE mall_lite;

CREATE TABLE IF NOT EXISTS pms_product_favorite
(
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品收藏ID',
    user_id           BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    product_id        BIGINT UNSIGNED NOT NULL COMMENT '商品SPU ID',
    active_product_id BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN deleted = 0 THEN product_id ELSE NULL END
    ) STORED COMMENT '未删除商品ID生成列，用于限制同一用户不能重复收藏同一商品',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_active_product (user_id, active_product_id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '商品收藏表';
