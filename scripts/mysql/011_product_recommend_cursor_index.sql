USE mall_lite;

SET @index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'pms_product'
      AND index_name = 'idx_pms_product_recommend_scroll'
);

SET @create_index_sql := IF(
    @index_exists = 0,
    'CREATE INDEX idx_pms_product_recommend_scroll ON pms_product(status, deleted, sort, id)',
    'SELECT 1'
);

PREPARE stmt FROM @create_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
