# Elasticsearch Query DSL 基础查询阶段设计

## 一、目标

本阶段在上一阶段“索引和文档 REST CRUD”之后继续推进，目标是让学习者能够使用 Elasticsearch REST API 编写基础商品搜索查询，包括全文搜索、精确过滤、范围过滤、排序、分页和组合查询。

## 二、范围

### 1. 包含内容

1. 新增 Query DSL 学习笔记，解释 `term`、`match`、`range`、`bool`、`filter`、`sort`、`from`、`size`、`_source` 等基础能力。
2. 新增可执行 `.http` 练习文件，使用独立索引 `mall_product_query_v1`，避免影响上一阶段 CRUD 索引。
3. 新增 Bulk 初始化数据文件，保证练习请求可以重复从头执行。

### 2. 不包含内容

1. 不接入 Spring Boot Java 客户端。
2. 不引入 IK 分词器。
3. 不实现聚合查询。
4. 不提交 Git commit，由用户自行提交。

## 三、文件设计

1. `docs/notes/elasticsearch-query-dsl-basic.md`
   - 负责说明 Query DSL 基础概念、查询类型、常见坑和阶段检查清单。
2. `docs/http/elasticsearch-query-dsl-basic.http`
   - 负责提供 IntelliJ IDEA 可直接运行的 REST API 练习请求。
3. `docs/http/elasticsearch-query-dsl-products.ndjson`
   - 负责提供 Bulk 初始化商品数据，请求体以换行符结尾，避免 Bulk API newline 报错。

## 四、验证标准

1. Markdown 文档具备清晰层级和中文编号。
2. `.http` 文件不包含通配符删除索引，只操作 `mall_product_query_v1`。
3. Bulk 数据文件最后一个字节必须是换行符 `10`。
4. 使用本地 Elasticsearch 执行初始化与代表性查询，确认返回 HTTP 200 且查询结果符合预期。
