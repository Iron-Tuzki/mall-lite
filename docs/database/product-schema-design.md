# 商品分类、SPU、SKU 表设计

## 一、设计目标

本阶段只设计商品域的三张基础表：

1. `pms_category`：商品分类表。
2. `pms_product`：商品 SPU 表。
3. `pms_sku`：商品 SKU 表。

这三张表用于支撑商品分类浏览、商品详情展示、SKU 选择和后续库存、订单链路。商品图片表、属性表、品牌表、评价表、搜索索引等扩展能力暂不纳入第一版。

## 二、核心概念

### 1. 分类

分类用于组织商品，例如：

```text
手机数码
└── 手机
    └── 智能手机
```

第一版使用 `parent_id` 实现简单树结构。

### 2. SPU

SPU 表示一个标准商品。例如：

```text
iPhone 15
```

SPU 保存商品公共信息，例如商品名称、分类、主图、描述、上下架状态。

### 3. SKU

SKU 表示具体可购买规格。例如：

```text
iPhone 15 黑色 128G
iPhone 15 蓝色 256G
```

SKU 保存具体价格、规格信息、SKU 编码、销售状态。后续库存表会按 `sku_id` 维护库存。

## 三、命名约定

商品域统一使用 `pms` 前缀，表示 Product Management System。

| 表名 | 说明 |
| --- | --- |
| `pms_category` | 商品分类表 |
| `pms_product` | 商品 SPU 表 |
| `pms_sku` | 商品 SKU 表 |

## 四、`pms_category` 商品分类表

### 1. 表作用

`pms_category` 用于维护商品分类树，支持前台分类展示和后台商品归类。

### 2. 字段设计

| 字段 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint unsigned` | 是 | 自增 | 分类 ID |
| `parent_id` | `bigint unsigned` | 是 | `0` | 父分类 ID，0 表示一级分类 |
| `name` | `varchar(64)` | 是 | 无 | 分类名称 |
| `level` | `tinyint unsigned` | 是 | `1` | 分类层级 |
| `sort` | `int` | 是 | `0` | 排序值，越小越靠前 |
| `icon_url` | `varchar(255)` | 否 | `NULL` | 分类图标 |
| `status` | `tinyint unsigned` | 是 | `1` | 状态：1 启用，0 禁用 |
| `create_time` | `datetime` | 是 | 当前时间 | 创建时间 |
| `update_time` | `datetime` | 是 | 当前时间 | 更新时间 |
| `deleted` | `tinyint unsigned` | 是 | `0` | 逻辑删除标记 |

### 3. 索引设计

| 索引名 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `idx_parent_id` | `parent_id` | 普通索引 | 查询子分类 |
| `idx_status_sort` | `status, sort` | 普通索引 | 查询启用分类并排序 |

## 五、`pms_product` 商品 SPU 表

### 1. 表作用

`pms_product` 保存商品公共信息，用于商品列表和商品详情基础展示。

### 2. 字段设计

| 字段 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint unsigned` | 是 | 自增 | 商品 ID |
| `category_id` | `bigint unsigned` | 是 | 无 | 分类 ID |
| `product_code` | `varchar(64)` | 是 | 无 | 商品编码，唯一 |
| `name` | `varchar(128)` | 是 | 无 | 商品名称 |
| `subtitle` | `varchar(255)` | 否 | `NULL` | 商品副标题 |
| `main_image_url` | `varchar(255)` | 否 | `NULL` | 商品主图 |
| `description` | `text` | 否 | `NULL` | 商品描述 |
| `status` | `tinyint unsigned` | 是 | `1` | 状态：1 上架，0 下架 |
| `sort` | `int` | 是 | `0` | 排序值 |
| `create_time` | `datetime` | 是 | 当前时间 | 创建时间 |
| `update_time` | `datetime` | 是 | 当前时间 | 更新时间 |
| `deleted` | `tinyint unsigned` | 是 | `0` | 逻辑删除标记 |

### 3. 索引设计

| 索引名 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `uk_product_code` | `product_code` | 唯一索引 | 保证商品编码唯一 |
| `idx_category_status` | `category_id, status` | 普通索引 | 按分类查询上架商品 |
| `idx_status_sort` | `status, sort` | 普通索引 | 查询上架商品并排序 |

## 六、`pms_sku` 商品 SKU 表

### 1. 表作用

`pms_sku` 保存具体可售规格。用户下单时最终选择的是 SKU，而不是 SPU。

### 2. 字段设计

| 字段 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint unsigned` | 是 | 自增 | SKU ID |
| `product_id` | `bigint unsigned` | 是 | 无 | 商品 SPU ID |
| `sku_code` | `varchar(64)` | 是 | 无 | SKU 编码，唯一 |
| `sku_name` | `varchar(128)` | 是 | 无 | SKU 名称 |
| `spec_data` | `json` | 否 | `NULL` | 规格数据，例如颜色、容量 |
| `price` | `decimal(10,2)` | 是 | 无 | 销售价 |
| `original_price` | `decimal(10,2)` | 否 | `NULL` | 原价 |
| `main_image_url` | `varchar(255)` | 否 | `NULL` | SKU 主图 |
| `status` | `tinyint unsigned` | 是 | `1` | 状态：1 可售，0 不可售 |
| `create_time` | `datetime` | 是 | 当前时间 | 创建时间 |
| `update_time` | `datetime` | 是 | 当前时间 | 更新时间 |
| `deleted` | `tinyint unsigned` | 是 | `0` | 逻辑删除标记 |

### 3. 索引设计

| 索引名 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `uk_sku_code` | `sku_code` | 唯一索引 | 保证 SKU 编码唯一 |
| `idx_product_status` | `product_id, status` | 普通索引 | 查询商品下可售 SKU |

## 七、表关系

```text
pms_category.id 1 ---- n pms_product.category_id
pms_product.id  1 ---- n pms_sku.product_id
```

第一版 SQL 中不强制添加外键，保持和用户域一致，由业务层、事务和测试保证关联数据正确。

## 八、生产注意点

1. 金额字段使用 `decimal(10,2)`，不要使用 `float` 或 `double`。
2. 商品是否可下单要同时检查 SPU 状态和 SKU 状态。
3. SKU 不直接保存库存数量，后续库存表会通过 `sku_id` 单独维护可用库存、锁定库存。
4. `spec_data` 使用 JSON 适合第一版快速表达规格，后续如果要支持复杂筛选，可以拆成规格属性表。
5. 商品图片第一版只保留主图，后续可以独立设计商品图片表。
