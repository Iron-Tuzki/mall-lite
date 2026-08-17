# Elasticsearch Query DSL 基础查询

> 版本假设：本笔记基于本地 Elasticsearch `7.17.23`。目标是从 REST API 角度掌握商品搜索最常用的 Query DSL，为后续接入 Spring Boot 商品搜索接口打基础。

## 一、为什么 CRUD 之后要学 Query DSL

### 1. Query DSL 解决什么问题

CRUD 负责把数据写进 Elasticsearch，但业务真正需要的是“按条件搜出来”。

在商城项目里，一个商品搜索通常不是单一条件，而是组合条件：

1. 用户输入关键词，例如“机械键盘”。
2. 只看上架商品。
3. 限定分类，例如电脑外设。
4. 限定价格区间。
5. 按相关性、价格、创建时间排序。
6. 分页返回结果。

Elasticsearch 的 **Query DSL** 就是用 JSON 表达这些搜索条件的查询语言。

### 2. 和 SQL 的直觉类比

可以先粗略理解为：

| SQL 能力 | Elasticsearch Query DSL |
|---|---|
| `SELECT *` | `match_all` |
| `WHERE status = 'ON_SALE'` | `term` / `filter` |
| `WHERE price BETWEEN 100 AND 500` | `range` |
| `LIKE '%键盘%'`，但更智能 | `match` |
| 多条件 `AND / OR / NOT` | `bool` |
| `ORDER BY price DESC` | `sort` |
| `LIMIT offset, size` | `from` / `size` |

这个类比只帮助入门。ES 和 MySQL 的底层模型不同：MySQL 以行表和 B+Tree 为核心，ES 以文档、倒排索引、相关性评分为核心。

## 二、查询请求的基本结构

### 1. 最常见的搜索入口

```http
GET mall_product_query_v1/_search
{
  "query": {
    "match_all": {}
  }
}
```

核心结构是：

```json
{
  "query": {
    "查询类型": {
      "字段名": "查询值"
    }
  }
}
```

### 2. 查询结果里要看什么

返回结果重点看：

| 字段 | 含义 |
|---|---|
| `took` | 查询耗时，单位毫秒 |
| `timed_out` | 查询是否超时 |
| `_shards` | 分片执行情况 |
| `hits.total.value` | 命中文档总数 |
| `hits.hits` | 命中的文档列表 |
| `_score` | 相关性评分 |
| `_source` | 原始业务文档 |

其中 `_score` 是 ES 搜索非常重要的概念：**全文搜索不是简单等于/不等于，而是根据词项匹配程度计算相关性。**

## 三、match_all、分页、排序和字段过滤

### 1. match_all：查询全部文档

```json
{
  "query": {
    "match_all": {}
  }
}
```

适合：

- 验证索引是否有数据。
- 管理后台浏览数据。
- 搭配 `from`、`size`、`sort` 做基础列表页。

不适合：

- 大数据量深分页。
- 没有过滤条件地扫大量线上数据。

### 2. from / size：普通分页

```json
{
  "from": 0,
  "size": 10,
  "query": {
    "match_all": {}
  }
}
```

含义：

1. `from`：从第几条开始，类似 SQL offset。
2. `size`：返回多少条。

生产注意：`from + size` 很大时会有深分页性能问题，因为每个分片都可能需要取出较多候选结果再合并。后续要学习 `search_after`。

### 3. sort：排序

```json
{
  "sort": [
    {
      "price": "asc"
    },
    {
      "createdAt": "desc"
    }
  ],
  "query": {
    "match_all": {}
  }
}
```

常见排序字段应使用：

- `keyword`
- `date`
- 数值类型，例如 `long`、`integer`、`scaled_float`

通常不要直接对 `text` 字段排序，因为 `text` 是分词后的全文字段。

### 4. _source：只返回需要的字段

```json
{
  "_source": [
    "productId",
    "productName",
    "price"
  ],
  "query": {
    "match_all": {}
  }
}
```

生产建议：搜索列表页只返回必要字段，减少网络传输和 JSON 解析成本。

## 四、term、terms：精确匹配查询

### 1. term：单值精确查询

```json
{
  "query": {
    "term": {
      "status": "ON_SALE"
    }
  }
}
```

适合查：

- 商品状态：`status`
- 商品编码：`productCode`
- 品牌：`brandName`
- 分类 ID：`categoryId`
- 标签：`tags`

### 2. terms：多值精确查询

```json
{
  "query": {
    "terms": {
      "categoryId": [
        20,
        30
      ]
    }
  }
}
```

含义类似 SQL：

```sql
WHERE category_id IN (20, 30)
```

### 3. 高频坑：不要随便对 text 字段用 term

如果字段是：

```json
"productName": {
  "type": "text"
}
```

那么 `productName` 会被分词。对 `text` 字段直接用 `term`，查询的是分词后的词项，不是完整原文。

如果你需要完整精确匹配商品名，可以给字段建 `.keyword` 子字段：

```json
"productName": {
  "type": "text",
  "fields": {
    "keyword": {
      "type": "keyword"
    }
  }
}
```

然后查：

```json
{
  "query": {
    "term": {
      "productName.keyword": "三模机械键盘"
    }
  }
}
```

## 五、match、multi_match：全文搜索查询

### 1. match：单字段全文搜索

```json
{
  "query": {
    "match": {
      "productName": "机械键盘"
    }
  }
}
```

`match` 会走分词流程：

1. 用户输入内容被 analyzer 分词。
2. ES 到倒排索引中查找词项。
3. 根据匹配情况计算 `_score`。
4. 按相关性返回结果。

适合：

- 商品名称搜索。
- 商品描述搜索。
- 文章标题、内容搜索。

### 2. multi_match：多字段全文搜索

```json
{
  "query": {
    "multi_match": {
      "query": "无线 外设",
      "fields": [
        "productName^3",
        "brandName^2",
        "description"
      ]
    }
  }
}
```

这里 `productName^3` 表示商品名称字段权重更高。命中商品名比只命中描述更重要。

商城搜索里很常见：

1. 商品名权重最高。
2. 品牌名次之。
3. 描述权重较低。

### 3. match 与 term 的核心区别

| 对比项 | `term` | `match` |
|---|---|---|
| 查询方式 | 精确词项匹配 | 全文搜索 |
| 是否分析查询文本 | 否 | 是 |
| 常用字段 | `keyword`、数值、日期、布尔 | `text` |
| 是否计算相关性 | 通常会，但多用于过滤 | 会，且很重要 |
| 典型场景 | 状态、分类、编码 | 名称、描述、文章内容 |

面试表达：

> `term` 查询不会对查询条件做分词，适合 keyword、数值、日期等精确匹配字段；`match` 查询会先对查询文本做分析，再到倒排索引中匹配，适合 text 类型的全文检索字段。

## 六、range：范围查询

### 1. 价格范围

```json
{
  "query": {
    "range": {
      "price": {
        "gte": 100,
        "lte": 500
      }
    }
  }
}
```

范围操作符：

| 操作符 | 含义 |
|---|---|
| `gt` | 大于 |
| `gte` | 大于等于 |
| `lt` | 小于 |
| `lte` | 小于等于 |

### 2. 时间范围

```json
{
  "query": {
    "range": {
      "createdAt": {
        "gte": "2026-06-01T00:00:00+08:00",
        "lte": "2026-06-30T23:59:59+08:00"
      }
    }
  }
}
```

生产中时间范围很常见：

- 日志检索。
- 订单查询。
- 商品上新时间。
- 活动有效期。

## 七、bool：组合查询的核心

### 1. bool 四个核心子句

| 子句 | 类似含义 | 是否影响评分 | 常见用途 |
|---|---|---|---|
| `must` | AND，必须匹配 | 是 | 关键词搜索 |
| `filter` | AND，必须过滤 | 否 | 状态、分类、价格 |
| `should` | OR，应该匹配 | 是 | 加权召回、可选条件 |
| `must_not` | NOT，必须不匹配 | 否 | 排除下架、排除黑名单 |

### 2. 商城搜索推荐写法

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "productName": "键盘"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "status": "ON_SALE"
          }
        },
        {
          "range": {
            "price": {
              "gte": 100,
              "lte": 500
            }
          }
        }
      ],
      "must_not": [
        {
          "term": {
            "tags": "清仓"
          }
        }
      ]
    }
  }
}
```

生产经验：

1. **关键词搜索放 `must`**，让 ES 计算相关性。
2. **结构化条件放 `filter`**，例如状态、分类、价格、品牌。
3. `filter` 不参与评分，通常更适合缓存和优化。
4. 不要把所有条件都塞进 `must`，否则评分计算可能没有必要地变复杂。

## 八、minimum_should_match：控制 should 命中数量

### 1. should 的默认行为容易误解

当 `bool` 查询里只有 `should`，通常至少要命中一个。

当同时存在 `must` 或 `filter` 时，`should` 默认更像“加分项”，不一定必须命中。

### 2. 强制至少命中一个 should

```json
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "status": "ON_SALE"
          }
        }
      ],
      "should": [
        {
          "match": {
            "productName": "键盘"
          }
        },
        {
          "match": {
            "description": "键盘"
          }
        }
      ],
      "minimum_should_match": 1
    }
  }
}
```

这个配置表示：商品必须上架，并且商品名或描述至少一个字段要命中“键盘”。

## 九、Query Context 和 Filter Context

### 1. Query Context

Query Context 关注：

1. 文档是否匹配。
2. 匹配得有多好。

会计算 `_score`。

典型例子：

```json
{
  "match": {
    "productName": "机械键盘"
  }
}
```

### 2. Filter Context

Filter Context 只关注：

1. 文档是否满足条件。
2. 不关心匹配得有多好。

不计算 `_score`。

典型例子：

```json
{
  "term": {
    "status": "ON_SALE"
  }
}
```

### 3. 后端开发中的选择建议

| 条件类型 | 推荐位置 |
|---|---|
| 用户关键词 | `must` |
| 商品状态 | `filter` |
| 分类 ID | `filter` |
| 品牌 | `filter` |
| 价格范围 | `filter` |
| 创建时间范围 | `filter` |
| 可选增强召回 | `should` |
| 排除条件 | `must_not` |

## 十、常见错误和排查方式

### 1. 查不到数据

优先检查：

1. 索引名是否正确。
2. 字段名是否正确。
3. 字段类型是 `text` 还是 `keyword`。
4. 使用的是 `term` 还是 `match`。
5. 是否被时间范围或其他过滤条件排除了。

排查命令：

```http
GET mall_product_query_v1/_mapping
```

```http
GET mall_product_query_v1/_search
{
  "query": {
    "match_all": {}
  }
}
```

### 2. 对 text 字段排序或聚合失败

通常原因：`text` 字段默认不适合排序和聚合。

解决方式：使用 `.keyword` 子字段。

```json
{
  "sort": [
    {
      "productName.keyword": "asc"
    }
  ]
}
```

### 3. 查询很慢

常见原因：

1. 深分页过大。
2. 无过滤条件扫描大量数据。
3. 对高基数字段做复杂聚合。
4. 查询字段 mapping 不合理。
5. 集群资源不足。

入门阶段先记住：**能过滤的结构化条件尽量放到 filter，列表页不要深分页。**

## 十一、阶段成果检查清单

完成本阶段后，你应该能独立写出：

1. 查询所有文档的 `match_all`。
2. 按状态、分类、品牌精确查询的 `term` / `terms`。
3. 按商品名称和描述全文搜索的 `match` / `multi_match`。
4. 按价格和时间范围查询的 `range`。
5. 同时组合关键词、状态、分类、价格的 `bool` 查询。
6. 使用 `_source` 控制返回字段。
7. 使用 `sort` 和 `from` / `size` 做普通分页。
8. 能解释 `term` 和 `match` 的区别。
9. 能解释 `must` 和 `filter` 的区别。

这一阶段学完，下一步就可以进入 **分词器、倒排索引和中文搜索**。那会解释为什么“机械键盘”能搜到哪些文档，以及中文搜索为什么通常需要 IK 分词器或更合适的 analyzer。
