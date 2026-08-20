USE mall_lite;

SET @product_brand_name_column_count = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'pms_product'
      AND column_name = 'brand_name'
);

SET @product_brand_name_sql = IF(
        @product_brand_name_column_count = 0,
        'ALTER TABLE pms_product ADD COLUMN brand_name VARCHAR(64) NULL COMMENT ''品牌名称'' AFTER name',
        'SELECT ''pms_product.brand_name already exists'' AS message'
                              );

PREPARE product_brand_name_stmt FROM @product_brand_name_sql;
EXECUTE product_brand_name_stmt;
DEALLOCATE PREPARE product_brand_name_stmt;
