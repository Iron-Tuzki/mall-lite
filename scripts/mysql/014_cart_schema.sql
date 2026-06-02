USE mall_lite;

CREATE TABLE IF NOT EXISTS oms_cart_item
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    sku_id      BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    quantity    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '数量，逻辑删除墓碑为0',
    version     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '购物车项版本号',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku (user_id, sku_id),
    KEY idx_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '购物车项表';
