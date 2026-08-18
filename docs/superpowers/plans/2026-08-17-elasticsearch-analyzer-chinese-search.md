# Elasticsearch Analyzer Chinese Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 Elasticsearch 分词器、倒排索引与中文搜索学习文档和 REST API 练习文件。

**Architecture:** 使用独立 Markdown 笔记讲解概念，使用独立 `.http` 文件提供可运行实验，使用独立 `.ndjson` 文件初始化商品数据。所有实验只操作 `mall_product_analyzer_v1`。

**Tech Stack:** Elasticsearch 7.17.23、Kibana Dev Tools、IntelliJ IDEA HTTP Client、Markdown。

## Global Constraints

1. 不提交代码，由用户自行提交。
2. 直接在当前分支修改，不创建隔离分支。
3. 概念性知识使用 Markdown 排版，要有明显分级和序号。
4. 默认练习不依赖 IK 插件，IK 请求仅作为可选示例。
5. 所有删除索引操作必须限定为 `mall_product_analyzer_v1`，禁止使用通配符。

---

### Task 1: 新增分词器与中文搜索学习笔记

**Files:**

- Create: `docs/notes/elasticsearch-analyzer-chinese-search.md`

**Interfaces:**

- Consumes: Query DSL 基础查询阶段成果。
- Produces: analyzer、倒排索引、中文搜索、IK 分词器和字段设计建议。

- [x] **Step 1: 编写 Markdown 笔记**

  使用中文编号二级标题，覆盖 analyzer 组成、倒排索引、写入分词、查询分词、`term` 与 `match`、中文搜索问题、IK 分词器理解。

- [x] **Step 2: 检查内容完整性**

  确认文档包含 `text` 与 `keyword` 区别、`.keyword` 子字段、`^数字` 权重说明、阶段成果检查清单。

### Task 2: 新增分词器 HTTP 实验文件

**Files:**

- Create: `docs/http/elasticsearch-analyzer-chinese-search.http`
- Create: `docs/http/elasticsearch-analyzer-products.ndjson`

**Interfaces:**

- Consumes: 本地 Elasticsearch `http://localhost:9200`。
- Produces: 可重复执行的分词器和中文搜索 REST API 实验。

- [x] **Step 1: 创建 `_analyze` 练习请求**

  覆盖 `standard`、`keyword`、`whitespace`，并提供 IK 可选示例。

- [x] **Step 2: 创建索引和 Mapping 请求**

  使用 `mall_product_analyzer_v1`，设置 `productName` 为 `text`，并添加 `keyword` 和 `whitespace` 子字段。

- [x] **Step 3: 创建查询练习请求**

  覆盖 `match`、`term`、`.keyword` 精确匹配、`multi_match` 权重和 `_explain`。

### Task 3: 验证文件和本地 Elasticsearch 请求

**Files:**

- Verify: `docs/notes/elasticsearch-analyzer-chinese-search.md`
- Verify: `docs/http/elasticsearch-analyzer-chinese-search.http`
- Verify: `docs/http/elasticsearch-analyzer-products.ndjson`

**Interfaces:**

- Consumes: 新增文档和本地 Elasticsearch。
- Produces: 可运行验证结果。

- [x] **Step 1: 文件格式检查**

  检查是否包含占位符、通配符删除、Bulk 文件末尾换行。

- [x] **Step 2: 本地请求验证**

  创建索引、Bulk 初始化、执行 `_analyze`、代表性查询并清理索引。
