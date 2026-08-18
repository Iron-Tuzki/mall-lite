# Elasticsearch Aggregations Basic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 Elasticsearch 聚合查询学习文档和 REST API 练习文件。

**Architecture:** 使用独立 Markdown 笔记讲解聚合概念，使用独立 `.http` 文件提供可运行实验，使用独立 `.ndjson` 文件初始化商品数据。所有实验只操作 `mall_product_agg_v1`。

**Tech Stack:** Elasticsearch 7.17.23、Kibana Dev Tools、IntelliJ IDEA HTTP Client、Markdown。

**Spec:** `docs/superpowers/specs/2026-08-18-elasticsearch-aggregations-basic-design.md`

## Global Constraints

1. 不提交代码，由用户自行提交。
2. 直接在当前分支修改，不创建隔离分支。
3. 概念性知识使用 Markdown 排版，要有明显分级和序号。
4. 所有删除索引操作必须限定为 `mall_product_agg_v1`，禁止使用通配符。

---

### Task 1: 新增聚合查询学习笔记

**Files:**

- Create: `docs/notes/elasticsearch-aggregations-basic.md`

**Interfaces:**

- Consumes: Query DSL 和分词器阶段学习成果。
- Produces: 聚合查询基础概念、商城筛选面板场景、生产建议和阶段检查清单。

- [x] **Step 1: 编写 Markdown 笔记**

  使用中文编号二级标题，覆盖 Bucket、Metric、`terms`、`range`、`histogram`、`date_histogram`、`query + aggs`、`filter`、`cardinality`。

- [x] **Step 2: 检查内容完整性**

  确认文档包含 `size: 0`、`doc_count`、`buckets`、`text` 字段聚合风险、`keyword` 字段建议。

### Task 2: 新增聚合 HTTP 实验文件

**Files:**

- Create: `docs/http/elasticsearch-aggregations-basic.http`
- Create: `docs/http/elasticsearch-aggregations-products.ndjson`

**Interfaces:**

- Consumes: 本地 Elasticsearch `http://localhost:9200`。
- Produces: 可重复执行的聚合查询 REST API 实验。

- [x] **Step 1: 创建索引和 Mapping 请求**

  使用 `mall_product_agg_v1`，聚合字段使用 `keyword`、数值和日期类型。

- [x] **Step 2: 创建 Bulk 初始化数据**

  写入 8 条商品数据，覆盖品牌、分类、价格、销量、库存、状态和创建时间。

- [x] **Step 3: 创建聚合练习请求**

  覆盖品牌统计、分类统计、价格指标、价格区间、日期分桶、过滤聚合、去重计数和搜索页综合聚合。

### Task 3: 验证文件和本地 Elasticsearch 请求

**Files:**

- Verify: `docs/notes/elasticsearch-aggregations-basic.md`
- Verify: `docs/http/elasticsearch-aggregations-basic.http`
- Verify: `docs/http/elasticsearch-aggregations-products.ndjson`

**Interfaces:**

- Consumes: 新增文档和本地 Elasticsearch。
- Produces: 可运行验证结果。

- [x] **Step 1: 文件格式检查**

  检查是否包含占位符、通配符删除、Bulk 文件末尾换行。

- [x] **Step 2: 本地请求验证**

  创建索引、Bulk 初始化、执行代表性聚合查询并清理索引。
