USE mall_lite;

INSERT INTO ums_user (id, username, password, nickname, phone, email, status, deleted)
VALUES (900001, 'seed_order_user', 'encoded-password', 'Seed Order User', '13900000001', 'seed-order@example.com', 1, 0)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    nickname = VALUES(nickname),
    status = VALUES(status),
    deleted = VALUES(deleted);

INSERT INTO ums_address (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, postal_code, default_flag, deleted)
VALUES (900001, 900001, 'Seed Receiver', '13900000001', 'Guangdong', 'Shenzhen', 'Nanshan', 'Seed Test Address', '518000', 1, 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    receiver_name = VALUES(receiver_name),
    receiver_phone = VALUES(receiver_phone),
    province = VALUES(province),
    city = VALUES(city),
    district = VALUES(district),
    detail_address = VALUES(detail_address),
    postal_code = VALUES(postal_code),
    default_flag = VALUES(default_flag),
    deleted = VALUES(deleted);

INSERT INTO pms_category (id, parent_id, name, level, sort, status, deleted)
VALUES (900001, 0, 'Seed Category', 1, 1, 1, 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    level = VALUES(level),
    sort = VALUES(sort),
    status = VALUES(status),
    deleted = VALUES(deleted);

INSERT INTO pms_product (id, category_id, product_code, name, subtitle, main_image_url, description, status, sort, deleted)
VALUES (900001, 900001, 'SEED-P-001', 'Seed Test Product', 'Seed product for integration tests',
        'https://example.com/seed-product.png', 'Seed product used by mall-lite tests.', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    name = VALUES(name),
    subtitle = VALUES(subtitle),
    main_image_url = VALUES(main_image_url),
    description = VALUES(description),
    status = VALUES(status),
    sort = VALUES(sort),
    deleted = VALUES(deleted);

INSERT INTO pms_sku (id, product_id, sku_code, sku_name, spec_data, price, original_price, main_image_url, status, deleted)
VALUES (900001, 900001, 'SEED-SKU-001', 'Seed Test SKU', JSON_OBJECT('color', 'black'), 199.00, 299.00,
        'https://example.com/seed-sku.png', 1, 0)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    sku_name = VALUES(sku_name),
    spec_data = VALUES(spec_data),
    price = VALUES(price),
    original_price = VALUES(original_price),
    main_image_url = VALUES(main_image_url),
    status = VALUES(status),
    deleted = VALUES(deleted);

INSERT INTO pms_sku (id, product_id, sku_code, sku_name, spec_data, price, original_price, main_image_url, status, deleted)
VALUES (900002, 900001, 'SEED-SKU-002', 'Seed Test SKU 2', JSON_OBJECT('color', 'white'), 59.00, 79.00,
        'https://example.com/seed-sku-2.png', 1, 0)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    sku_name = VALUES(sku_name),
    spec_data = VALUES(spec_data),
    price = VALUES(price),
    original_price = VALUES(original_price),
    main_image_url = VALUES(main_image_url),
    status = VALUES(status),
    deleted = VALUES(deleted);

INSERT INTO ims_inventory (id, sku_id, available_stock, locked_stock, version, deleted)
VALUES (900001, 900001, 1000, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    available_stock = VALUES(available_stock),
    locked_stock = VALUES(locked_stock),
    version = VALUES(version),
    deleted = VALUES(deleted);

INSERT INTO ims_inventory (id, sku_id, available_stock, locked_stock, version, deleted)
VALUES (900002, 900002, 1000, 0, 0, 0)
ON DUPLICATE KEY UPDATE
    available_stock = VALUES(available_stock),
    locked_stock = VALUES(locked_stock),
    version = VALUES(version),
    deleted = VALUES(deleted);
