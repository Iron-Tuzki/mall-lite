# Elasticsearch 基础概念与 REST CRUD 学习阶段设计

## 一、目标与范围

本阶段以本机 Elasticsearch 7.17.23 和 Kibana 7.17.23 为运行环境，帮助学习者建立 Elasticsearch 基础认知，并能够独立使用 REST API 完成索引和文档的增删改查。

本阶段只操作独立练习索引，不接入 Spring Boot，不同步 MySQL 商品数据，也不修改 `C:\MySoftware` 下的 Elasticsearch 与 Kibana 安装文件。

完成后应具备以下能力：

1. 解释 Elasticsearch、Lucene、Kibana 之间的关系。
2. 理解集群、节点、索引、文档、字段、Mapping、分片和副本。
3. 说明倒排索引、分词与近实时搜索的基本原理。
4. 区分 Elasticsearch 与 MySQL 的定位和典型使用场景。
5. 使用 REST API 完成索引和文档的完整 CRUD 生命周期。

## 二、运行环境

1. Elasticsearch：`C:\MySoftware\elasticsearch-7.17.23`
2. Kibana：`C:\MySoftware\kibana-7.17.23-windows-x86_64`
3. Elasticsearch 默认地址：`http://localhost:9200`
4. Kibana 默认地址：`http://localhost:5601`
5. REST 请求执行工具：IntelliJ IDEA HTTP Client

当前阶段使用 Elasticsearch 7.17.23 的 REST API 语义。后续接入 Spring Boot 时，需要重新核对 Spring Data Elasticsearch、Java 客户端与服务端版本兼容关系，不能直接把本阶段的版本假设延伸到 Java 集成阶段。

## 三、交付内容

### 1. 基础知识文档

新增 `docs/notes/elasticsearch-basic-concepts.md`，主要内容包括：

1. Elasticsearch 解决的问题和适用边界。
2. Elasticsearch、Lucene 与 Kibana 的关系。
3. 核心数据模型和集群模型。
4. Mapping、字段类型及动态映射风险。
5. 倒排索引、Analyzer、Token 与近实时搜索。
6. 分片、副本、路由和基础读写流程。
7. Elasticsearch 与 MySQL 的对比及商城使用方式。
8. 常见误区、生产注意事项和面试表达。
9. 本地启动、健康检查和 REST 实验说明。

文档使用中文 Markdown 分级排版，主章节采用中文序号，并通过表格和 JSON 示例辅助理解。

### 2. 可执行 REST 请求文件

新增 `docs/http/elasticsearch-basic-crud.http`，以变量保存服务地址，并按可重复执行的学习顺序组织请求。

请求覆盖：

1. 查看节点信息与集群健康状态。
2. 删除可能残留的同名练习索引，保证实验可重复开始。
3. 显式设置 Mapping 后创建索引 `mall_product_basic_v1`。
4. 查看索引信息和 Mapping。
5. 为 Mapping 新增允许追加的字段。
6. 使用指定 ID 创建文档。
7. 使用自动 ID 创建文档。
8. 按 ID 查询文档。
9. 使用 `PUT` 全量替换文档。
10. 使用 `_update` 局部更新文档。
11. 使用 `_bulk` 批量写入文档。
12. 查询索引中的文档，观察写入结果。
13. 删除单个文档并验证删除结果。
14. 删除练习索引并验证清理结果。

每组请求附有中文注释，说明操作目的、预期响应和常见状态码。实验请求只访问 `localhost`，不会连接外部服务。

## 四、数据模型

练习索引 `mall_product_basic_v1` 模拟商城商品搜索文档，初始字段如下：

| 字段 | 类型 | 用途 |
| --- | --- | --- |
| `productId` | `long` | 商品唯一标识 |
| `productName` | `text` | 用于全文检索的商品名称 |
| `productCode` | `keyword` | 用于精确匹配和聚合的商品编码 |
| `categoryId` | `long` | 分类标识 |
| `price` | `scaled_float` | 商品价格，使用缩放因子保存精度 |
| `status` | `keyword` | 商品状态 |
| `tags` | `keyword` | 标签列表 |
| `description` | `text` | 商品描述 |
| `createdAt` | `date` | 创建时间 |

Mapping 采用显式定义，避免动态映射将字段识别为不合适的类型。练习中的 Mapping 修改只演示新增字段；已经存在字段的类型不可直接原地修改，类型调整需要新建索引并迁移数据。

## 五、执行流程

1. 启动 Elasticsearch，访问 `http://localhost:9200` 确认节点响应。
2. 按需启动 Kibana，用 Dev Tools 对照执行请求。
3. 阅读基础概念文档，理解每个请求操作的数据模型。
4. 在 IntelliJ IDEA 中打开 `.http` 文件，自上而下逐条执行。
5. 对照响应中的 `acknowledged`、`result`、`_version`、`found` 和 HTTP 状态码判断执行结果。
6. 执行清理请求删除练习索引，恢复到实验前状态。

## 六、错误处理与安全边界

1. 开始实验前删除同名练习索引，删除请求允许索引不存在，保证重复执行不会被残留数据阻塞。
2. 创建、更新和批量操作使用有效 JSON，并为 `_bulk` 请求保留必需的换行格式。
3. 文档明确区分 `PUT /index/_doc/id` 的覆盖语义与 `_update` 的局部更新语义。
4. 对查询不存在的文档、重复创建索引和字段类型冲突给出解释，不将预期的 `404` 或 `400` 简单视为环境故障。
5. 所有写操作仅针对名称固定的练习索引 `mall_product_basic_v1`，不使用通配符删除索引。
6. 不在请求文件中保存用户名、密码或其他密钥。

## 七、验证标准

### 1. 文档质量验证

1. Markdown 标题层级清晰，没有未完成占位符。
2. 概念、命令和版本假设一致。
3. REST 示例能够与 `.http` 文件中的实际请求对应。
4. 包含面试表达与生产风险，但不超出基础阶段范围。

### 2. REST 实验验证

1. Elasticsearch 根地址能够正常返回节点信息。
2. 创建索引返回 `acknowledged: true`。
3. Mapping 与预期字段类型一致。
4. 创建、全量替换和局部更新分别返回符合语义的 `result`。
5. 批量请求不存在解析错误，且各子操作执行成功。
6. 删除文档后查询得到 `found: false` 或 `404`。
7. 删除索引后检查确认索引不存在。

## 八、阶段边界

以下内容留到后续阶段：

1. Spring Boot 与 Java 客户端集成。
2. 中文分词插件安装与复杂分词配置。
3. 商品全文检索、筛选、排序、高亮和聚合。
4. MySQL 全量导入与 RabbitMQ 增量同步。
5. 索引别名、零停机重建和生产集群运维。

