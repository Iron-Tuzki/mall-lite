# Spring Boot 集成 Elasticsearch 商品搜索

> 版本假设：本项目使用 Spring Boot `3.5.13`、Java `21`，本地 Elasticsearch 为 `7.17.23`。本阶段采用 **Elasticsearch Low Level REST Client + 手写 Query DSL JSON** 的学习型集成方案，重点打通 Java 后端调用 ES 的完整链路。

## 一、为什么先用低层 REST Client

### 1. 阶段目标

前四个阶段已经分别掌握：

1. 索引和文档 CRUD。
2. Query DSL 基础查询。
3. 分词器、倒排索引和中文搜索。
4. Aggregations 聚合查询。

阶段 5 的目标是把这些能力接到 Spring Boot：

```text
前端/HTTP 请求
  ↓
Spring MVC Controller
  ↓
ProductSearchService
  ↓
Elasticsearch RestClient
  ↓
Elasticsearch _search
  ↓
解析 hits 和 aggregations
  ↓
返回商品列表和筛选面板
```

### 2. 为什么不是一上来就用 Repository

本地 ES 是 `7.17.23`，项目是 Spring Boot `3.5.13`。Spring Boot 3.x 的 Spring Data Elasticsearch 生态更偏向新版 Elasticsearch Java Client。为了降低版本兼容和抽象层学习成本，本阶段先使用低层 REST Client。

这样你能直接看到：

1. Java 代码如何构造 Query DSL。
2. Java 代码如何发送 `_search` 请求。
3. Java 代码如何解析 `hits`。
4. Java 代码如何解析 `aggregations`。

这条路线更像“先把发动机打开看清楚”，后续再切到更高层客户端会更踏实。

## 二、本阶段新增的后端结构

### 1. 配置层

核心文件：

```text
mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/config/ElasticsearchProperties.java
mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/config/ElasticsearchConfig.java
```

配置项位于：

```yaml
mall:
  elasticsearch:
    uris:
      - http://localhost:9200
    product-index: mall_product_agg_v1
    connect-timeout-millis: 3000
    socket-timeout-millis: 5000
```

### 2. 查询构造层

核心文件：

```text
ProductSearchQueryBuilder
```

它负责把：

```text
keyword、categoryId、brandName、minPrice、maxPrice、pageNo、pageSize
```

转换成 Elasticsearch `_search` 请求体。

核心策略：

1. `keyword` 使用 `multi_match`。
2. `status=ON_SALE` 固定放入 `filter`。
3. `categoryId`、`brandName`、价格范围放入 `filter`。
4. 返回品牌、分类、价格区间聚合。
5. 按 `_score`、`sales`、`price` 排序。

### 3. 响应解析层

核心文件：

```text
ProductSearchResultMapper
```

它负责把 ES 返回的：

1. `hits.total.value`
2. `hits.hits[*]._source`
3. `aggregations.by_brand.buckets`
4. `aggregations.by_category.buckets`
5. `aggregations.price_ranges.buckets`

转换成后端 VO。

### 4. 接口层

核心接口：

```http
GET /api/products/search
```

支持参数：

| 参数 | 含义 |
|---|---|
| `keyword` | 商品搜索关键词 |
| `categoryId` | 分类 ID |
| `brandName` | 品牌名 |
| `minPrice` | 最低价格 |
| `maxPrice` | 最高价格 |
| `pageNo` | 页码，从 1 开始 |
| `pageSize` | 每页数量，最大限制为 50 |

## 三、核心 Query DSL 结构

### 1. 后端构造的查询模型

```json
{
  "from": 0,
  "size": 10,
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "键盘",
            "fields": [
              "productName^3",
              "brandName^2",
              "description"
            ]
          }
        }
      ],
      "filter": [
        {
          "term": {
            "status": "ON_SALE"
          }
        }
      ]
    }
  },
  "aggs": {
    "by_brand": {
      "terms": {
        "field": "brandName",
        "size": 20
      }
    },
    "by_category": {
      "terms": {
        "field": "categoryName",
        "size": 20
      }
    },
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "key": "100 元以下",
            "to": 100
          },
          {
            "key": "100-500 元",
            "from": 100,
            "to": 500
          },
          {
            "key": "500 元以上",
            "from": 500
          }
        ]
      }
    }
  }
}
```

### 2. 为什么结构化条件放 filter

商品状态、分类、品牌和价格区间都属于结构化条件：

1. 它们不需要计算相关性。
2. 只需要判断是否满足条件。
3. 放在 `filter` 里语义更清晰。
4. ES 更容易优化过滤条件。

关键词放 `must`，因为它需要影响 `_score`。

## 四、接口调用示例

### 1. 搜索键盘

```http
GET /api/products/search?keyword=键盘&pageNo=1&pageSize=10
```

### 2. 搜索某分类下的键盘

```http
GET /api/products/search?keyword=键盘&categoryId=20&pageNo=1&pageSize=10
```

### 3. 搜索某价格范围内的商品

```http
GET /api/products/search?keyword=键盘&minPrice=100&maxPrice=800&pageNo=1&pageSize=10
```

## 五、生产注意事项

### 1. 索引数据同步还没有实现

当前阶段只实现“查 ES”。商品数据如何从 MySQL 同步到 ES，后续还要单独做。

常见方案：

1. 管理后台保存商品后同步写 ES。
2. 商品变更后发 MQ，异步写 ES。
3. 通过定时任务或批处理重建索引。
4. 使用 Canal 监听 MySQL binlog 同步。

### 2. ES 不应该成为唯一数据源

商品详情、下单、库存扣减仍然应该以 MySQL 为准。ES 更适合：

1. 搜索。
2. 筛选。
3. 排序。
4. 聚合统计。

### 3. 接口降级要考虑清楚

当前实现中，ES 请求失败会返回业务异常：

```text
elasticsearch search failed
```

生产里可以进一步做：

1. 超时控制。
2. Sentinel 降级。
3. 兜底查 MySQL 简单列表。
4. 搜索接口失败告警。

## 六、阶段成果检查清单

完成本阶段后，你应该能做到：

1. 解释 Spring Boot 如何创建 ES RestClient。
2. 解释 Controller、Service、QueryBuilder、ResultMapper 的职责边界。
3. 用 Java 构造 `multi_match + filter + aggs` 查询。
4. 解析 ES `hits` 为商品列表。
5. 解析 ES `aggregations` 为筛选面板。
6. 知道当前阶段还没有解决 MySQL 到 ES 的数据同步问题。

下一阶段建议进入 **商品数据同步到 Elasticsearch**，把管理后台商品新增、修改、上下架和删除与 ES 索引数据联动起来。
