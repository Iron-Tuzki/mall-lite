# Elasticsearch Query DSL Basic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 Elasticsearch Query DSL 基础学习文档和 REST API 练习文件。

**Architecture:** 使用独立学习笔记承载概念，使用独立 `.http` 文件承载可执行实验，使用独立 `.ndjson` 文件承载 Bulk 初始化数据。所有实验只操作 `mall_product_query_v1`。

**Tech Stack:** Elasticsearch 7.17.23、Kibana Dev Tools、IntelliJ IDEA HTTP Client、Markdown。

## Global Constraints

1. 不提交代码，由用户自行提交。
2. 直接在当前分支修改，不创建隔离分支。
3. 概念性知识使用 Markdown 排版，要有明显分级和序号。
4. 所有删除索引操作必须限定为 `mall_product_query_v1`，禁止使用通配符。

---

### Task 1: 新增 Query DSL 学习笔记

**Files:**

- Create: `docs/notes/elasticsearch-query-dsl-basic.md`

**Interfaces:**

- Consumes: 上一阶段 Elasticsearch CRUD 学习成果。
- Produces: Query DSL 基础概念、查询类型、生产建议和阶段检查清单。

- [x] **Step 1: 编写 Markdown 笔记**

  使用中文编号二级标题，覆盖 `match_all`、`term`、`terms`、`match`、`multi_match`、`range`、`bool`、`filter`、分页、排序和 `_source`。

- [x] **Step 2: 检查内容完整性**

  确认文档中包含 `term` 和 `match` 区别、`must` 和 `filter` 区别、商城搜索组合查询建议。

### Task 2: 新增 Query DSL HTTP 实验文件

**Files:**

- Create: `docs/http/elasticsearch-query-dsl-basic.http`
- Create: `docs/http/elasticsearch-query-dsl-products.ndjson`

**Interfaces:**

- Consumes: 本地 Elasticsearch `http://localhost:9200`。
- Produces: 可重复执行的 Query DSL REST API 实验。

- [x] **Step 1: 创建索引和 Mapping 请求**

  使用 `mall_product_query_v1`，设置 `number_of_shards=1`、`number_of_replicas=0`、`dynamic=strict`。

- [x] **Step 2: 创建 Bulk 初始化数据**

  写入 6 条商品数据，覆盖键盘、鼠标、耳机、显示器、上架、下架、清仓等搜索场景。

- [x] **Step 3: 创建查询练习请求**

  覆盖 `match_all`、`_source`、`from/size`、`sort`、`term`、`terms`、`match`、`multi_match`、`range`、`bool`、`minimum_should_match`。

### Task 3: 验证文件和本地 Elasticsearch 请求

**Files:**

- Verify: `docs/notes/elasticsearch-query-dsl-basic.md`
- Verify: `docs/http/elasticsearch-query-dsl-basic.http`
- Verify: `docs/http/elasticsearch-query-dsl-products.ndjson`

**Interfaces:**

- Consumes: 新增文档和本地 Elasticsearch。
- Produces: 可运行验证结果。

- [x] **Step 1: 文件格式检查**

  检查是否包含占位符、通配符删除、Bulk 文件末尾换行。

- [x] **Step 2: 本地请求验证**

  创建索引、Bulk 初始化、执行代表性查询、清理索引。
