# Elasticsearch Aggregations 聚合查询基础

> 版本假设：本笔记基于本地 Elasticsearch `7.17.23`。本阶段目标是掌握商城搜索中最常见的聚合查询：分类统计、品牌统计、价格区间统计、数值指标统计和日期趋势统计。

## 一、为什么阶段 4 要学聚合查询

### 1. 聚合解决什么业务问题

Query DSL 解决“哪些商品符合条件”，Aggregations 解决“这些商品按维度统计后是什么样”。

在商城搜索页里，用户搜索“键盘”之后，页面右侧或顶部通常会出现筛选面板：

1. 分类：电脑外设 12 个，数码影音 3 个。
2. 品牌：KeyMaster 5 个，GamePro 2 个。
3. 价格区间：0-100、100-500、500-1000。
4. 状态：上架、下架。
5. 平均价格、最高价格、最低价格。

这些就很适合用 ES 聚合查询完成。

### 2. 聚合和 SQL 的类比

| SQL | Elasticsearch |
|---|---|
| `GROUP BY brand_name` | `terms` aggregation |
| `COUNT(*)` | 每个 bucket 的 `doc_count` |
| `AVG(price)` | `avg` aggregation |
| `MIN(price)` | `min` aggregation |
| `MAX(price)` | `max` aggregation |
| `SUM(sales)` | `sum` aggregation |
| `WHERE status = 'ON_SALE'` 后再统计 | `query` + `aggs` |

注意：这个类比只是帮助理解。ES 聚合基于倒排索引、列式 doc values 和分片归并，和 MySQL 的执行模型不一样。

## 二、聚合的基本结构

### 1. aggs 写在哪里

聚合查询写在请求体的 `aggs` 或 `aggregations` 字段里：

```json
{
  "size": 0,
  "aggs": {
    "by_brand": {
      "terms": {
        "field": "brandName"
      }
    }
  }
}
```

常见写法会加：

```json
"size": 0
```

含义是：**不返回具体文档，只返回聚合结果**。做统计面板时很常见。

### 2. 返回结果怎么看

结果一般在：

```json
"aggregations": {
  "by_brand": {
    "buckets": [
      {
        "key": "KeyMaster",
        "doc_count": 3
      }
    ]
  }
}
```

重点看：

| 字段 | 含义 |
|---|---|
| `key` | 分组值，例如品牌名 |
| `doc_count` | 当前分组命中文档数量 |
| `buckets` | 桶列表 |
| `value` | 指标聚合结果，例如平均值 |

## 三、Bucket Aggregation 和 Metric Aggregation

### 1. Bucket：先分桶

Bucket 聚合负责“分组”。

常见 Bucket 聚合：

| 聚合 | 作用 |
|---|---|
| `terms` | 按关键词、状态、分类、品牌分组 |
| `range` | 按数值范围分组 |
| `histogram` | 按固定数值间隔分组 |
| `date_histogram` | 按日期间隔分组 |
| `filter` | 只统计满足某个过滤条件的数据 |

### 2. Metric：算指标

Metric 聚合负责“计算”。

常见 Metric 聚合：

| 聚合 | 作用 |
|---|---|
| `avg` | 平均值 |
| `min` | 最小值 |
| `max` | 最大值 |
| `sum` | 求和 |
| `stats` | 一次返回 count、min、max、avg、sum |
| `cardinality` | 去重计数，近似值 |

### 3. 两者经常嵌套使用

例如：按品牌分组，并统计每个品牌的平均价格：

```json
{
  "size": 0,
  "aggs": {
    "by_brand": {
      "terms": {
        "field": "brandName"
      },
      "aggs": {
        "avg_price": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

这就是“先分桶，再算指标”。

## 四、terms 聚合：商城筛选面板最常用

### 1. 按品牌统计

```json
{
  "size": 0,
  "aggs": {
    "by_brand": {
      "terms": {
        "field": "brandName",
        "size": 10
      }
    }
  }
}
```

适合字段：

1. `keyword`
2. 数值枚举
3. 布尔值
4. 状态字段

不建议直接对 `text` 字段做 terms 聚合。商品名称如果要聚合，应使用：

```text
productName.keyword
```

### 2. terms 聚合的 size

`terms.size` 表示返回前多少个桶，不是返回多少文档。

```json
{
  "terms": {
    "field": "brandName",
    "size": 5
  }
}
```

表示返回商品数量最多的前 5 个品牌。

## 五、range 和 histogram：价格区间统计

### 1. range：自定义区间

```json
{
  "size": 0,
  "aggs": {
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "to": 100
          },
          {
            "from": 100,
            "to": 500
          },
          {
            "from": 500
          }
        ]
      }
    }
  }
}
```

适合商城价格筛选：

1. 100 元以下。
2. 100 到 500 元。
3. 500 元以上。

### 2. histogram：固定间隔分桶

```json
{
  "size": 0,
  "aggs": {
    "price_histogram": {
      "histogram": {
        "field": "price",
        "interval": 500
      }
    }
  }
}
```

适合观察价格分布，但商城前台筛选更常用 `range`，因为运营通常希望自定义区间。

## 六、date_histogram：按时间统计

### 1. 按天统计商品创建数量

```json
{
  "size": 0,
  "aggs": {
    "created_by_day": {
      "date_histogram": {
        "field": "createdAt",
        "calendar_interval": "day",
        "format": "yyyy-MM-dd"
      }
    }
  }
}
```

常见用途：

1. 每天新增商品数量。
2. 每小时日志数量。
3. 每月订单数量。
4. 每天搜索请求量。

### 2. calendar_interval 和 fixed_interval

| 参数 | 含义 | 示例 |
|---|---|---|
| `calendar_interval` | 按日历单位分桶，会考虑自然日、自然月 | `day`、`month` |
| `fixed_interval` | 按固定时长分桶 | `1h`、`30m` |

商品创建日期按天或月统计，通常使用 `calendar_interval`。

## 七、query + aggs：先筛选再统计

### 1. 只统计上架商品

```json
{
  "size": 0,
  "query": {
    "term": {
      "status": "ON_SALE"
    }
  },
  "aggs": {
    "by_brand": {
      "terms": {
        "field": "brandName"
      }
    }
  }
}
```

这个查询的语义是：

```text
先找出 status = ON_SALE 的商品
再在这些商品里按 brandName 分组统计
```

### 2. 搜索结果页筛选面板

真实商城搜索里经常是：

```text
关键词搜索 + 条件过滤 + 聚合统计
```

例如用户搜“键盘”，页面同时返回：

1. 商品列表。
2. 当前搜索结果下的品牌统计。
3. 当前搜索结果下的价格区间统计。
4. 当前搜索结果下的分类统计。

## 八、filter 聚合：单独统计某个条件

### 1. 统计上架商品的平均价格

```json
{
  "size": 0,
  "aggs": {
    "on_sale_products": {
      "filter": {
        "term": {
          "status": "ON_SALE"
        }
      },
      "aggs": {
        "avg_price": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

`filter` 聚合适合在同一个请求里单独统计某类数据。

## 九、常见坑和生产建议

### 1. text 字段不能随便聚合

错误倾向：

```json
{
  "terms": {
    "field": "productName"
  }
}
```

更推荐：

```json
{
  "terms": {
    "field": "productName.keyword"
  }
}
```

原因是 `text` 字段会分词，聚合出来的是 token，不是完整商品名。

### 2. cardinality 是近似去重

```json
{
  "cardinality": {
    "field": "brandName"
  }
}
```

它适合大数据量下估算去重数量，但不是绝对精确计数。生产上要知道这个语义，别把它当强一致财务报表。

### 3. 聚合会消耗资源

聚合不是免费的，特别是：

1. 高基数字段，例如用户 ID、订单号。
2. 大范围时间查询。
3. 多层嵌套聚合。
4. 很大的 `terms.size`。

生产建议：

1. 搜索页聚合要限制时间范围或查询范围。
2. 聚合字段优先使用 `keyword`、数值、日期。
3. `terms.size` 不要无脑调大。
4. 面向报表的大聚合可以考虑离线统计或专门的分析索引。

## 十、阶段成果检查清单

完成本阶段后，你应该能做到：

1. 解释 Bucket 聚合和 Metric 聚合的区别。
2. 使用 `terms` 按品牌、分类、状态统计。
3. 使用 `avg`、`min`、`max`、`sum`、`stats` 统计数值指标。
4. 使用 `range` 做价格区间统计。
5. 使用 `histogram` 观察价格分布。
6. 使用 `date_histogram` 按时间分桶。
7. 写出 `query + aggs`，理解“先筛选再聚合”。
8. 解释为什么聚合通常要用 `keyword` 字段。

下一阶段建议进入 **Spring Boot 集成 Elasticsearch**，把前面 REST API 学到的索引、查询、聚合能力逐步封装成 Java 后端接口。
