# Elasticsearch Spring Boot Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Spring Boot 后端新增基于 Elasticsearch 的商品搜索接口。

**Architecture:** `ProductSearchController` 接收 HTTP 参数并组装请求 DTO，`ProductSearchService` 使用低层 REST Client 调用 ES，`ProductSearchQueryBuilder` 构造 Query DSL，`ProductSearchResultMapper` 解析 hits 和 aggregations。默认查询索引为 `mall_product_agg_v1`。

**Tech Stack:** Spring Boot 3.5.13、Java 21、Elasticsearch REST Client 7.17.23、Jackson、JUnit 5。

**Spec:** `docs/superpowers/specs/2026-08-19-elasticsearch-spring-boot-search-design.md`

## Global Constraints

1. 不提交代码，由用户自行提交。
2. 直接在当前分支修改，不创建隔离分支。
3. Java 类必须提供中文 JavaDoc 说明类作用。
4. Java 接口必须提供中文 JavaDoc 说明接口作用及参数含义。
5. 不删除用户已有注释。

---

### Task 1: 添加依赖和配置

**Files:**

- Modify: `mall-backend/pom.xml`
- Modify: `mall-backend/mall-product/pom.xml`
- Modify: `mall-backend/mall-app/src/main/resources/application.yml`

**Interfaces:**

- Consumes: Spring Boot 多模块 Maven 配置。
- Produces: 可注入的 Elasticsearch RestClient 配置。

- [x] **Step 1: 添加 Elasticsearch REST Client 版本管理**

  在父 POM 中添加 `elasticsearch-rest-client.version=7.17.23` 和依赖管理。

- [x] **Step 2: mall-product 引入 REST Client**

  在 `mall-product` 模块引入 `org.elasticsearch.client:elasticsearch-rest-client`。

- [x] **Step 3: application.yml 添加 ES 配置**

  添加 `mall.elasticsearch.uris`、`product-index`、连接超时和读取超时。

### Task 2: 测试先行定义 Query DSL 构造

**Files:**

- Create: `mall-backend/mall-product/src/test/java/com/tuzki/mall/product/search/ProductSearchQueryBuilderTest.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/ProductSearchQueryBuilder.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/dto/ProductSearchRequest.java`

**Interfaces:**

- Consumes: 商品搜索请求 DTO。
- Produces: Elasticsearch `_search` JSON 请求体。

- [x] **Step 1: 编写失败测试**

  测试关键词、分类、品牌、价格、分页、排序和聚合 JSON。

- [x] **Step 2: 实现最小 QueryBuilder**

  使用 Jackson `ObjectNode` 构造 `multi_match + filter + aggs + sort`。

- [x] **Step 3: 运行测试**

  执行 `mvn -f mall-backend/pom.xml -pl mall-product test "-Dtest=ProductSearchQueryBuilderTest"`。

### Task 3: 测试先行定义响应解析

**Files:**

- Create: `mall-backend/mall-product/src/test/java/com/tuzki/mall/product/search/ProductSearchResultMapperTest.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/ProductSearchResultMapper.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/vo/ProductSearchItemVO.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/vo/ProductSearchAggVO.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/vo/ProductSearchResultVO.java`

**Interfaces:**

- Consumes: Elasticsearch `_search` JSON 响应。
- Produces: 商品搜索结果 VO。

- [x] **Step 1: 编写失败测试**

  测试 hits、品牌聚合、分类聚合和价格区间聚合解析。

- [x] **Step 2: 实现响应映射器和 VO**

  解析 `hits.total.value`、`hits.hits[*]._source` 和 `aggregations.*.buckets`。

- [x] **Step 3: 运行测试**

  执行 `mvn -f mall-backend/pom.xml -pl mall-product test "-Dtest=ProductSearchQueryBuilderTest,ProductSearchResultMapperTest"`。

### Task 4: 暴露后端搜索接口

**Files:**

- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/service/ProductSearchService.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/search/service/impl/ElasticsearchProductSearchService.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/product/controller/ProductSearchController.java`

**Interfaces:**

- Consumes: HTTP 查询参数和 ES 配置。
- Produces: `GET /api/products/search`。

- [x] **Step 1: 新增服务接口和实现**

  服务通过低层 REST Client 调用 `/{productIndex}/_search`。

- [x] **Step 2: 新增 Controller**

  Controller 接收查询参数，组装 `ProductSearchRequest`，返回 `Result<ProductSearchResultVO>`。

### Task 5: 文档和验证

**Files:**

- Create: `docs/notes/elasticsearch-spring-boot-integration.md`
- Create: `docs/http/elasticsearch-spring-boot-search.http`

**Interfaces:**

- Consumes: 阶段 5 后端实现。
- Produces: 学习笔记和接口调用示例。

- [x] **Step 1: 新增学习笔记**

  说明配置、QueryBuilder、ResultMapper、Service、Controller 和生产边界。

- [x] **Step 2: 新增接口 HTTP 练习**

  提供 `/api/products/search` 的关键词、分类、品牌、价格区间调用示例。

- [x] **Step 3: 运行最终验证**

  运行模块测试、编译检查、静态格式检查并记录结果。
