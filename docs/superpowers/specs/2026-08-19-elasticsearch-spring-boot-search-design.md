# Spring Boot 集成 Elasticsearch 商品搜索阶段设计

## 一、目标

本阶段在 Elasticsearch REST API 学习之后，将商品搜索能力接入 Spring Boot 后端，提供一个基于 Elasticsearch 的商品搜索接口，支持关键词搜索、结构化过滤、分页排序和筛选聚合。

## 二、范围

### 1. 包含内容

1. 在 `mall-product` 模块新增 Elasticsearch 配置、搜索 DTO、VO、Query DSL 构建器、响应映射器和搜索服务。
2. 在 `mall-app` 模块新增商品搜索 Controller。
3. 在 `application.yml` 中新增 `mall.elasticsearch` 配置。
4. 新增阶段学习笔记和接口练习 `.http` 文件。
5. 使用单元测试覆盖 Query DSL 构造和 ES 响应解析。

### 2. 不包含内容

1. 不实现 MySQL 商品数据同步到 ES。
2. 不实现 IK 分词器安装和索引迁移。
3. 不实现搜索高亮。
4. 不改前端页面。
5. 不提交 Git commit，由用户自行提交。

## 三、技术方案

采用 Elasticsearch Low Level REST Client 访问本地 ES `7.17.23`。查询 JSON 由 `ProductSearchQueryBuilder` 构造，搜索响应由 `ProductSearchResultMapper` 解析，`ProductSearchService` 对 Controller 提供稳定接口。

## 四、验证标准

1. `ProductSearchQueryBuilderTest` 通过，证明关键词、过滤、聚合和排序 JSON 构造符合预期。
2. `ProductSearchResultMapperTest` 通过，证明 hits 和 aggregations 可解析为后端 VO。
3. `mall-product` 模块测试通过。
4. `mall-app` 模块至少完成编译验证。
5. 新增 Java 类和接口具备中文 JavaDoc。
