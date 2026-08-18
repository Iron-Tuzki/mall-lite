# Elasticsearch 聚合查询阶段设计

## 一、目标

本阶段在分词器与中文搜索之后继续推进，目标是让学习者掌握 Elasticsearch Aggregations 基础能力，能够用 REST API 完成商城搜索页常见的品牌、分类、价格区间、数值指标和日期趋势统计。

## 二、范围

### 1. 包含内容

1. 新增聚合查询学习笔记。
2. 新增可执行 `.http` 练习文件，使用独立索引 `mall_product_agg_v1`。
3. 新增 Bulk 初始化数据文件，覆盖品牌、分类、价格、销量、库存、状态和创建时间等聚合场景。
4. 覆盖 `terms`、`avg`、`min`、`max`、`sum`、`stats`、`range`、`histogram`、`date_histogram`、`filter`、`cardinality` 和 `query + aggs`。

### 2. 不包含内容

1. 不实现 Java 后端接口。
2. 不接入 Kibana Dashboard 自动化配置。
3. 不讲复杂 pipeline aggregation。
4. 不提交 Git commit，由用户自行提交。

## 三、文件设计

1. `docs/notes/elasticsearch-aggregations-basic.md`
   - 解释聚合查询、Bucket 聚合、Metric 聚合、商城筛选面板场景和生产注意事项。
2. `docs/http/elasticsearch-aggregations-basic.http`
   - 提供创建索引、Bulk 初始化和常见聚合查询练习。
3. `docs/http/elasticsearch-aggregations-products.ndjson`
   - 提供本阶段 Bulk 初始化商品数据，文件末尾保留换行。

## 四、验证标准

1. Markdown 文档具备清晰层级和中文编号。
2. `.http` 文件只操作 `mall_product_agg_v1`，删除索引不使用通配符。
3. Bulk 数据文件最后一个字节必须是换行符 `10`。
4. 本地 Elasticsearch 能执行创建索引、Bulk 写入、代表性聚合查询并清理索引。
