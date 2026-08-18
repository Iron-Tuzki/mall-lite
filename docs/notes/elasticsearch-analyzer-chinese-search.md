# Elasticsearch 分词器、倒排索引与中文搜索

> 版本假设：本笔记基于本地 Elasticsearch `7.17.23`。本阶段目标是理解 analyzer 如何影响倒排索引和搜索结果，尤其是中文商品搜索为什么容易出现“看起来应该命中，但实际效果一般”的情况。

## 一、为什么阶段 3 要学分词器

### 1. 从“会查”到“知道为什么查得到”

上一阶段 Query DSL 解决的是“怎么写查询条件”。但真正决定全文搜索效果的，还有一个更底层的问题：

> 文本在写入 Elasticsearch 时，被切成了哪些词？查询时，又被切成了哪些词？

如果写入时和查询时的分词结果不符合业务预期，就会出现这些现象：

1. 搜“机械键盘”能查到，但排序不理想。
2. 搜“蓝牙键盘”命中了很多只包含“蓝牙”或“键盘”的商品。
3. 搜商品完整名称时，`term` 查不到，`match` 却能查到。
4. 中文短语没有按“词语”理解，而是被拆成单个字。

这些都和 **Analyzer 分词器** 有关。

### 2. 本阶段的核心心智模型

全文搜索的链路可以这样理解：

```text
文档写入
  ↓
字段 analyzer 分词
  ↓
生成倒排索引：词项 -> 文档列表
  ↓
用户查询
  ↓
查询文本 analyzer 分词
  ↓
拿查询词项去倒排索引中找文档
  ↓
计算相关性评分并返回结果
```

所以，**不是原文直接和原文比较，而是 token 和 token 匹配**。

## 二、Analyzer 是什么

### 1. Analyzer 的组成

Analyzer 通常由三部分组成：

| 组件 | 作用 | 示例 |
|---|---|---|
| Character Filter | 分词前预处理字符 | 去掉 HTML 标签、替换字符 |
| Tokenizer | 把文本切成 token | standard、keyword、whitespace |
| Token Filter | 对 token 再处理 | lowercase、stop、synonym |

执行顺序：

```text
原始文本
  ↓
Character Filter
  ↓
Tokenizer
  ↓
Token Filter
  ↓
最终 token 列表
```

### 2. 常见内置 analyzer

| analyzer | 大致行为 | 适合场景 |
|---|---|---|
| `standard` | 标准分词，英文按词，中文通常按单字处理 | 默认全文字段 |
| `keyword` | 整段文本作为一个 token | 编码、状态、完整名称精确匹配 |
| `whitespace` | 按空白字符切分 | 已经人为用空格分好的文本 |
| `simple` | 按非字母字符切分并转小写 | 简单英文场景 |

入门阶段先抓住一句话：

> `text` 字段会分词，`keyword` 字段不分词。

## 三、倒排索引是什么

### 1. 正排和倒排的区别

普通文档存储更像正排：

```text
文档 1 -> 三模机械键盘
文档 2 -> 无线静音鼠标
文档 3 -> 电竞机械键盘
```

倒排索引则反过来：

```text
键 -> 文档 1、文档 3
盘 -> 文档 1、文档 3
鼠 -> 文档 2
标 -> 文档 2
```

如果使用更适合中文的分词器，理想情况下可能更接近：

```text
机械键盘 -> 文档 1、文档 3
无线鼠标 -> 文档 2
电竞 -> 文档 3
```

### 2. 为什么倒排索引适合搜索

用户搜索“键盘”时，ES 不需要逐条扫描所有商品描述，而是直接查倒排索引中“键盘”对应的文档列表。

这就是 ES 适合全文检索的根本原因之一。

## 四、写入分词和查询分词

### 1. index analyzer

文档写入时使用的 analyzer，决定倒排索引里存哪些 token。

示例：

```json
{
  "productName": "三模机械键盘"
}
```

如果 `productName` 使用 `standard` analyzer，中文经常会被拆成较细的 token。最终倒排索引里保存的是分词结果，而不是简单保存完整原文用于全文匹配。

### 2. search analyzer

用户查询时使用的 analyzer，决定查询文本被拆成哪些 token。

```json
{
  "match": {
    "productName": "机械键盘"
  }
}
```

这个查询会先把“机械键盘”分析成 token，再拿这些 token 去倒排索引中匹配。

### 3. 两边 analyzer 要协调

如果写入时按单字切，查询时按词切，可能导致查不到或效果差。

如果写入时按词切，查询时也按词切，通常更符合中文搜索直觉。

生产建议：

1. 商品名称、标题、描述使用适合业务语言的全文 analyzer。
2. 商品编码、状态、枚举、品牌 ID 使用 `keyword`。
3. 需要精确匹配完整商品名时，使用 `.keyword` 子字段。

## 五、term 和 match 在分词场景下的区别

### 1. match 会分析查询文本

```json
{
  "query": {
    "match": {
      "productName": "机械键盘"
    }
  }
}
```

`match` 会对“机械键盘”先分词，再查询倒排索引。

### 2. term 不分析查询文本

```json
{
  "query": {
    "term": {
      "productName": "机械键盘"
    }
  }
}
```

`term` 会把“机械键盘”当成一个完整词项去倒排索引里找。如果索引里没有完整的“机械键盘”这个 token，就查不到。

这就是很多 ES 新手的高频坑：

> 对 `text` 字段直接用 `term`，经常不是你想要的结果。

### 3. 精确匹配完整值应该用 keyword

```json
{
  "query": {
    "term": {
      "productName.keyword": "三模机械键盘"
    }
  }
}
```

`.keyword` 不分词，适合完整值精确匹配、排序、聚合。

## 六、中文搜索的典型问题

### 1. standard analyzer 对中文不够懂业务语义

Elasticsearch 默认 `standard` analyzer 对英文比较自然，比如：

```text
Wireless Mechanical Keyboard
```

可以按单词理解。

但中文没有天然空格：

```text
三模机械键盘
```

默认分词不一定能理解：

```text
三模 / 机械键盘
```

它可能切得更碎。这样依然能搜索，但相关性和语义效果通常不如中文专用分词器。

### 2. 中文搜索常见改善方向

| 方向 | 作用 |
|---|---|
| 使用中文分词器 | 让“机械键盘”“蓝牙耳机”等词更符合中文语义 |
| 建立多字段 | 同一个字段同时支持全文、精确、不同 analyzer |
| 使用同义词 | “手机”“移动电话”互相召回 |
| 合理加权 | 商品名权重高于品牌和描述 |
| 搜索词纠错 | 处理错别字、拼写错误 |

在国内 Java 后端项目里，常见中文分词方案是 IK 分词器，例如 `ik_smart`、`ik_max_word`。不过它不是 ES 默认内置能力，需要安装插件，并且插件版本必须和 ES 版本匹配。

## 七、IK 分词器的理解

### 1. ik_smart 和 ik_max_word

| 分词器 | 特点 | 适合场景 |
|---|---|---|
| `ik_smart` | 粗粒度切分，结果更少 | 搜索时减少噪声 |
| `ik_max_word` | 细粒度切分，尽量多切词 | 建索引时提高召回 |

常见搭配：

```json
{
  "analyzer": "ik_max_word",
  "search_analyzer": "ik_smart"
}
```

大致思路：

1. 写入时多切一些词，提高被搜到的机会。
2. 查询时切得更稳一些，减少过度召回。

### 2. 插件版本必须匹配

如果本地 ES 是：

```text
7.17.23
```

IK 插件也要找兼容 `7.17.23` 的版本。版本不匹配可能导致 ES 启动失败，这是生产里很硬的坑。

## 八、商城商品搜索中的字段设计建议

### 1. 商品名称

推荐：

```json
"productName": {
  "type": "text",
  "analyzer": "standard",
  "search_analyzer": "standard",
  "fields": {
    "keyword": {
      "type": "keyword",
      "ignore_above": 256
    }
  }
}
```

如果后续安装 IK，可以改成：

```json
"productName": {
  "type": "text",
  "analyzer": "ik_max_word",
  "search_analyzer": "ik_smart",
  "fields": {
    "keyword": {
      "type": "keyword",
      "ignore_above": 256
    }
  }
}
```

### 2. 品牌、状态、编码

推荐使用 `keyword`：

```json
"brandName": {
  "type": "keyword"
}
```

原因：

1. 品牌通常用于筛选、聚合、排序。
2. 状态、编码是结构化值，不应该被分词。
3. 精确字段用 `keyword` 性能和语义更稳定。

### 3. 描述字段

描述字段一般内容更长，适合全文搜索，但权重通常低于商品名称。

常见查询：

```json
{
  "multi_match": {
    "query": "无线键盘",
    "fields": [
      "productName^3",
      "brandName^2",
      "description"
    ]
  }
}
```

这里的权重由 `^数字` 决定，**不是字段顺序决定**。

## 九、常见排查命令

### 1. 查看 analyzer 分词结果

```http
POST _analyze
{
  "analyzer": "standard",
  "text": "三模机械键盘"
}
```

重点看返回里的：

```json
"tokens": []
```

### 2. 查看字段实际使用的 mapping

```http
GET mall_product_analyzer_v1/_mapping
```

重点看：

1. 字段类型是不是 `text`。
2. 有没有 `.keyword` 子字段。
3. `analyzer` 和 `search_analyzer` 是什么。

### 3. 查看查询为什么命中

```http
GET mall_product_analyzer_v1/_explain/3001
{
  "query": {
    "match": {
      "productName": "机械键盘"
    }
  }
}
```

`_explain` 可以帮助你理解某个文档为什么命中、分数是怎么来的。生产排查相关性问题时很有用，但线上不要对大批量文档滥用。

## 十、阶段成果检查清单

完成本阶段后，你应该能做到：

1. 解释 analyzer 的三个组成部分。
2. 解释倒排索引为什么适合全文搜索。
3. 说清楚写入分词和查询分词的区别。
4. 解释为什么 `match` 能查到而 `term` 可能查不到。
5. 解释 `text` 和 `keyword` 的区别。
6. 使用 `_analyze` 查看文本分词结果。
7. 使用 `_mapping` 检查字段分词配置。
8. 理解中文搜索为什么通常需要中文分词器。
9. 知道 IK 分词器需要和 ES 版本匹配。

下一阶段建议进入 **聚合查询 Aggregations**，学习分类统计、品牌统计、价格区间统计，为商城筛选面板做准备。
