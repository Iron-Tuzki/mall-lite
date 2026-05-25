package com.tuzki.mall;

/**
 * 测试种子数据常量，和 scripts/mysql/005_test_seed_data.sql 中的数据保持一致。
 */
public final class TestSeedData {

    public static final Long USER_ID = 900001L;

    public static final Long ADDRESS_ID = 900001L;

    public static final Long CATEGORY_ID = 900001L;

    public static final Long PRODUCT_ID = 900001L;

    public static final Long SKU_ID = 900001L;

    public static final String SKU_CODE = "SEED-SKU-001";

    public static final String PRODUCT_NAME = "Seed Test Product";

    public static final String SKU_NAME = "Seed Test SKU";

    public static final String RECEIVER_NAME = "Seed Receiver";

    private TestSeedData() {
    }
}
