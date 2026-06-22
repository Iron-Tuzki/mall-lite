# Elasticsearch 基础概念与 REST CRUD

> 本文基于 **Elasticsearch 7.17.23** 和 **Kibana 7.17.23**，贯穿案例是 `mall-lite` 商城商品搜索。当前阶段只学习基础概念以及索引、文档的 REST CRUD，不涉及 Spring Boot 客户端集成。

## 一、Elasticsearch 解决什么问题

### 1. 先建立直观理解

Elasticsearch 是一个基于 Lucene 构建的**分布式搜索与分析引擎**。它把数据保存为 JSON 文档，并为适合搜索的字段建立索引，从而支持全文检索、条件过滤、排序、聚合和近实时分析。

可以把它理解为：

```text
业务 JSON 数据
    ↓ 建立索引
词项、字段值与文档位置等检索结构
    ↓ 查询
快速找到相关文档并计算匹配程度
```

MySQL 更擅长事务和结构化数据管理；Elasticsearch 更擅长从大量文档中进行搜索和分析。两者在商城中通常是**配合关系**，不是简单替代关系。

### 2. 商城为什么需要 Elasticsearch

假设用户搜索“无线机械键盘”，还要求：

- 商品名称、描述、标签可以参与全文检索；
- 按分类、品牌、上下架状态过滤；
- 按价格、销量或相关性排序；
- 高亮命中的关键词；
- 统计每个品牌和价格区间的商品数量。

MySQL 可以完成部分简单查询，但 `%关键词%` 模糊匹配通常难以有效利用普通 B+Tree 索引，也不擅长分词、相关性评分和大规模文本检索。Elasticsearch 正是为这些检索需求设计的。

### 3. 适合与不适合的场景

| 场景 | 是否适合 | 原因 |
| --- | --- | --- |
| 商品全文搜索 | 适合 | 支持倒排索引、分词和相关性评分 |
| 日志检索与聚合 | 适合 | 擅长大量文档的过滤和统计 |
| 搜索建议、筛选、高亮 | 适合 | 搜索能力丰富 |
| 订单扣款、库存扣减 | 不适合作为事实源 | 这类业务依赖严格事务和一致性 |
| 高频单行主键更新 | 通常不优先 | Elasticsearch 的更新本质上会重新索引文档 |
| 多表复杂事务 | 不适合 | 不提供关系数据库式的跨表事务能力 |

**生产优先推荐：MySQL 保存最终业务事实，Elasticsearch 保存面向检索的文档副本。**

## 二、Elasticsearch、Lucene 与 Kibana

### 1. 三者的职责

| 组件 | 核心职责 | 当前项目中的作用 |
| --- | --- | --- |
| Elasticsearch | 分布式存储、搜索和分析，对外提供 REST API | 保存并查询商品搜索文档 |
| Lucene | 单机搜索引擎库，负责倒排索引、分词和底层检索 | 被 Elasticsearch 封装使用 |
| Kibana | Elasticsearch 的查询、可视化和运维界面 | 使用 Dev Tools 调试请求、观察集群 |

### 2. 它们之间的关系

```text
浏览器 / IntelliJ HTTP Client / Java 应用
                    ↓ REST API
             Elasticsearch 集群
                    ↓ 封装与调度
             每个分片对应 Lucene 索引

Kibana ───────────→ Elasticsearch REST API
```

Elasticsearch 解决了 Lucene 本身没有直接提供的分布式能力，例如集群、分片、副本、节点发现和 REST API。Kibana 只是 Elasticsearch 的客户端和管理界面，**业务索引数据并不保存在 Kibana 中**。

## 三、核心数据模型

### 1. Index：索引

**Index** 是一组具有相近结构和用途的 JSON 文档集合，例如：

```text
mall_product_basic_v1
```

这个名称表示商城商品基础练习索引，`v1` 为以后通过新索引迁移 Mapping 留出版本空间。

索引不仅包含文档，还包含：

- Mapping；
- 分片、副本等 Settings；
- Analyzer 等文本分析配置。

不要把 Elasticsearch Index 和 MySQL B+Tree 索引完全等同。它更接近一个“可独立配置和搜索的文档集合”。

### 2. Document：文档

**Document** 是 Elasticsearch 中的基本数据单元，使用 JSON 表示：

```json
{
  "productId": 1001,
  "productName": "无线机械键盘",
  "productCode": "KEYBOARD-1001",
  "categoryId": 20,
  "price": 299.00,
  "status": "ON_SALE"
}
```

文档拥有 `_index`、`_id`、`_version` 等元数据。业务字段保存在 `_source` 中；默认情况下查询单个文档时看到的原始 JSON 就来自 `_source`。

### 3. Field：字段

**Field** 是文档中的属性。一个字段可以：

- 被索引，用于搜索；
- 被保存在 `_source`，用于返回原始值；
- 建立 `doc_values`，用于排序和聚合；
- 通过 Mapping 指定数据类型和分析方式。

### 4. Mapping：映射

**Mapping** 定义字段的类型及其索引方式，类似数据库表结构，但不能机械地等同于关系表 DDL。

Elasticsearch 7.x 的 REST API 统一使用 `_doc`，不再为一个索引设计多个业务 Mapping Type。商品、订单等结构差异明显的数据通常应放在不同索引中。

### 5. 与关系数据库概念的近似对照

| Elasticsearch | MySQL 近似概念 | 注意事项 |
| --- | --- | --- |
| Index | 表或同类数据集合 | 只是帮助理解，能力边界不同 |
| Document | 一行记录 | 文档是可嵌套的 JSON |
| Field | 列 | 同一字段可有不同索引方式 |
| Mapping | 表结构 | 字段类型修改限制更强 |
| `_id` | 主键 | 可由客户端指定或自动生成 |

## 四、集群、节点、分片与副本

### 1. Cluster 与 Node

**Cluster（集群）**由一个或多个 **Node（节点）**组成。每个节点都是一个 Elasticsearch 进程，可以承担保存数据、协调请求、选举集群管理节点等职责。

当前本地学习环境通常只有一个节点。单节点足以完成 REST API 学习，但不能验证真正的节点故障转移。

### 2. Primary Shard：主分片

一个 Index 会被拆成若干 **Primary Shard（主分片）**。每个分片底层都是一个独立的 Lucene 索引。

写入文档时，Elasticsearch 根据 `_routing` 计算目标主分片；默认 `_routing` 使用文档 `_id`。请求先由主分片处理，再复制到副本分片。

主分片数量会影响：

- 数据如何分布；
- 可并行处理能力；
- 单分片大小；
- 集群元数据和资源开销。

**常见误区：分片不是越多越好。** 每个分片都会消耗文件句柄、内存、CPU 和集群管理资源。本地练习索引使用 1 个主分片即可。

### 3. Replica Shard：副本分片

**Replica Shard（副本分片）**是主分片的复制品，主要用于：

1. 主分片所在节点故障时提供容错；
2. 分担读请求，提高部分查询吞吐量。

副本不会和对应主分片分配在同一个节点。若单节点索引设置 1 个副本，该副本无法分配，集群通常显示 `yellow`。本阶段把副本数设为 0，使练习索引在单节点环境下保持 `green`。

### 4. 基础写入与读取流程

写入流程可以简化为：

```text
客户端请求
  → 接收请求的协调节点
  → 根据 routing 定位主分片
  → 主分片执行写入
  → 同步到副本分片
  → 返回结果
```

按 `_id` 读取文档可以直接路由到对应分片；搜索请求通常需要向索引的相关分片广播查询，再由协调节点归并结果。这也是分片数量直接影响查询开销的原因之一。

## 五、Mapping 与字段类型

### 1. 为什么推荐显式 Mapping

Elasticsearch 可以根据首批数据动态推断字段类型，但推断结果未必符合业务语义。例如商品编码可能被当作可分词字符串，日期格式也可能被错误识别。

生产环境通常优先显式定义核心字段 Mapping：

```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "productName": { "type": "text" },
      "productCode": { "type": "keyword" },
      "price": {
        "type": "scaled_float",
        "scaling_factor": 100
      }
    }
  }
}
```

`dynamic: strict` 会拒绝 Mapping 中不存在的字段，适合用来尽早发现字段拼写错误和不受控的数据结构变化。

### 2. 常用字段类型

| 类型 | 典型用途 | 示例 |
| --- | --- | --- |
| `text` | 分词后的全文检索 | 商品名称、商品描述 |
| `keyword` | 精确匹配、排序、聚合 | 商品编码、状态、品牌 |
| `long` / `integer` | 整数 | 商品 ID、库存数量 |
| `scaled_float` | 按比例缩放的小数 | 学习示例中的商品价格 |
| `boolean` | 布尔状态 | 是否推荐 |
| `date` | 时间范围和排序 | 创建时间 |
| `object` | 普通嵌套 JSON 对象 | 简单规格对象 |
| `nested` | 需要保持数组内对象独立关系 | SKU 属性组合 |

金额作为最终交易事实时仍应由 MySQL 的 `DECIMAL` 等精确类型保存。本例使用 `scaled_float` 是为了学习搜索文档中的价格筛选和排序，并不改变 MySQL 的事实源地位。

### 3. `text` 与 `keyword` 是高频考点

- `text` 默认经过 Analyzer，保存分词后的词项，适合 `match` 全文查询；
- `keyword` 把整个值作为一个词项，适合 `term` 精确查询、排序和聚合；
- 对 `text` 字段直接做聚合通常不是正确选择，常见做法是增加 `keyword` 子字段。

### 4. Mapping 修改限制

可以通过 `PUT /索引名/_mapping` **新增字段**，但已经存在字段的类型通常不能直接原地修改。例如不能把 `price` 从 `keyword` 直接改成 `scaled_float`。

生产中的常见做法是：

1. 创建新版本索引；
2. 使用正确 Mapping；
3. 迁移或重新构建数据；
4. 通过别名切换读写入口；
5. 验证后再清理旧索引。

## 六、倒排索引、分词与近实时搜索

### 1. 什么是倒排索引

传统理解中，文档保存“文档 → 内容”。倒排索引额外维护“词项 → 文档”的映射。

假设有两个商品：

```text
文档 1：无线机械键盘
文档 2：无线鼠标
```

概念上的倒排结果类似：

```text
无线 → 文档 1、文档 2
机械 → 文档 1
键盘 → 文档 1
鼠标 → 文档 2
```

搜索“无线 键盘”时，不需要逐行扫描所有原始商品描述，而是从词项快速定位候选文档，再计算相关性。

### 2. Analyzer 的处理过程

Analyzer 通常由三类组件组成：

1. **Character Filter**：分词前处理字符，例如去除 HTML；
2. **Tokenizer**：把文本切分为 Token；
3. **Token Filter**：统一大小写、去停用词或转换词形。

```text
原始文本 → Character Filter → Tokenizer → Token Filter → Terms
```

索引时和查询时使用的分析策略需要相互兼容，否则用户输入的词项可能无法匹配索引中的词项。

Elasticsearch 自带的标准分词器对英文更友好。中文商品搜索通常需要后续单独学习中文分词方案；本阶段不安装插件，避免把环境配置和基础 CRUD 混在一起。

### 3. 为什么叫近实时搜索

文档写入成功后，可以按 `_id` 实时读取，但普通搜索通常要等待 Refresh 后才能看到新数据。Elasticsearch 默认会周期性 Refresh 活跃索引，把新数据变成可搜索的 Segment，因此被称为 **Near Real-Time（近实时）**。

实验请求使用：

```text
refresh=wait_for
```

它表示等待下一次 Refresh 使本次变更可搜索，比在每次写入后强制 `refresh=true` 更温和。**生产中不要为了“立即可见”而无脑强制 Refresh**，频繁 Refresh 会增加 Segment 和合并压力。

### 4. 更新为什么也有成本

Lucene Segment 写入后基本不可变。Elasticsearch 更新文档时，并不是在原位置修改 JSON，而是标记旧版本已删除并重新索引新版本，之后再由合并过程清理旧数据。

因此，Elasticsearch 更适合搜索和分析，而不是替代数据库承担高频、细粒度事务更新。

## 七、Elasticsearch 与 MySQL 的区别

### 1. 核心差异

| 维度 | Elasticsearch | MySQL |
| --- | --- | --- |
| 核心定位 | 搜索、分析、文档检索 | 关系数据、事务处理 |
| 数据模型 | JSON 文档 | 表、行、列、关系 |
| 典型索引 | 倒排索引、doc values | B+Tree 等 |
| 全文检索 | 核心能力 | 能支持，但不是所有场景的首选 |
| 事务 | 不提供关系数据库式跨文档事务 | 支持 ACID 事务 |
| Join | 能力受限，应控制使用 | 支持关系连接 |
| 更新方式 | 文档重新索引 | 可原地更新行数据页中的记录结构 |
| 一致性定位 | 搜索副本通常接受最终一致 | 核心业务事实通常要求更强一致性 |

### 2. 在 `mall-lite` 中如何分工

推荐的数据流是：

```text
后台创建或修改商品
        ↓
MySQL 事务成功，保存商品事实
        ↓
可靠事件或消息通知
        ↓
构建 Elasticsearch 商品搜索文档
        ↓
前台搜索读取 Elasticsearch
```

如果 Elasticsearch 暂时不可用，商品交易事实仍在 MySQL 中；系统可以重试同步、执行对账，或对搜索功能降级。**不能因为 Elasticsearch 写入失败就丢失商品事实。**

### 3. 面试选型表达

> MySQL 负责商品、库存和订单等需要事务与强约束的业务事实；Elasticsearch 保存为搜索场景反范式设计的文档副本，负责分词、相关性排序、过滤和聚合。两边通常通过可靠消息实现最终一致，并通过重试、幂等和定时对账修复数据偏差。

## 八、本地环境启动与验证

### 1. 当前安装位置

```text
Elasticsearch：C:\MySoftware\elasticsearch-7.17.23
Kibana：       C:\MySoftware\kibana-7.17.23-windows-x86_64
```

当前配置未显式覆盖端口时，默认地址为：

- Elasticsearch：`http://localhost:9200`
- Kibana：`http://localhost:5601`

### 2. Windows 启动命令

在 PowerShell 中启动 Elasticsearch：

```powershell
& 'C:\MySoftware\elasticsearch-7.17.23\bin\elasticsearch.bat'
```

按需在另一个 PowerShell 窗口启动 Kibana：

```powershell
& 'C:\MySoftware\kibana-7.17.23-windows-x86_64\bin\kibana.bat'
```

这两个前台进程运行时不要关闭对应终端。当前阶段也可以只启动 Elasticsearch，然后用 IntelliJ HTTP Client 执行请求。

### 3. 健康检查

打开以下地址：

```text
http://localhost:9200/
```

响应中的 `version.number` 应为 `7.17.23`。还可以查询：

```http
GET http://localhost:9200/_cluster/health
```

常见状态：

- `green`：所有主分片和副本分片都已分配；
- `yellow`：主分片已分配，但至少一个副本未分配，单节点加副本时很常见；
- `red`：至少一个主分片未分配，需要停止写入实验并排查。

## 九、索引 CRUD

以下请求都可以在 [REST CRUD 请求文件](../http/elasticsearch-basic-crud.http) 中逐条运行。

### 1. 创建索引

```http
PUT /mall_product_basic_v1
```

创建请求会同时定义 Settings 和 Mapping。成功时通常返回：

```json
{
  "acknowledged": true,
  "shards_acknowledged": true,
  "index": "mall_product_basic_v1"
}
```

- `acknowledged` 表示集群已确认索引创建操作；
- `shards_acknowledged` 表示所需分片已在超时前启动；
- 重复创建同名索引会返回 HTTP 400 和 `resource_already_exists_exception`。

### 2. 查询索引与 Mapping

```http
GET /mall_product_basic_v1
GET /mall_product_basic_v1/_mapping
```

第一个请求返回 Settings、Mapping 和别名信息；第二个请求只聚焦 Mapping。

### 3. 修改 Mapping

```http
PUT /mall_product_basic_v1/_mapping
```

本阶段用它新增 `brandName` 字段。新增字段成功通常返回 `acknowledged: true`，但不能用该接口把已有字段从一种类型直接改成另一种类型。

### 4. 删除索引

```http
DELETE /mall_product_basic_v1
```

删除索引会删除其中的文档和索引结构。**生产环境禁止使用未经约束的通配符删除命令。** 本实验只删除名称固定的练习索引。

## 十、文档 CRUD

### 1. 使用指定 ID 创建文档

```http
PUT /mall_product_basic_v1/_doc/1001?refresh=wait_for
```

首次写入通常返回：

```json
{
  "_index": "mall_product_basic_v1",
  "_id": "1001",
  "_version": 1,
  "result": "created"
}
```

对同一个 `_id` 再次执行 `PUT` 会完整写入新的 `_source`，`result` 通常变为 `updated`，`_version` 增加。这里的“更新”底层仍是重新索引文档。

如果业务要求“ID 已存在时必须失败”，可以学习后续的 `_create` API，而不能把普通 `PUT _doc/{id}` 当作仅创建操作。

### 2. 使用自动 ID 创建文档

```http
POST /mall_product_basic_v1/_doc?refresh=wait_for
```

Elasticsearch 会生成 `_id`。自动 ID 写入方便，但在业务系统中仍要考虑消息重试和幂等；否则同一业务事件重复消费可能生成多份文档。

### 3. 按 ID 查询文档

```http
GET /mall_product_basic_v1/_doc/1001
```

文档存在时：

- HTTP 状态码为 200；
- `found` 为 `true`；
- `_source` 保存业务字段。

文档不存在时通常返回 HTTP 404，且 `found` 为 `false`。

### 4. 全量写入与局部更新

全量写入：

```http
PUT /mall_product_basic_v1/_doc/1001?refresh=wait_for
```

局部更新：

```http
POST /mall_product_basic_v1/_update/1001?refresh=wait_for

{
  "doc": {
    "price": 279.00,
    "status": "ON_SALE"
  }
}
```

区别是：

- `PUT _doc/{id}` 接收一份新的完整 `_source`；没有再次提供的旧字段不会保留；
- `_update` 可以通过 `doc` 合并局部字段，但底层仍会产生一份重新索引后的文档；
- Mapping 并不会强制每个字段必填，业务必填约束仍需要应用层校验。

### 5. Bulk 批量写入

```http
POST /_bulk?refresh=wait_for
Content-Type: application/x-ndjson
```

Bulk 使用 NDJSON：一行操作元数据，一行文档数据，并且最后必须保留换行。

HTTP 200 **不等于每个子操作都成功**，还必须检查顶层 `errors` 以及每个 `items` 元素中的 `status` 和 `error`。这是生产批量同步非常容易踩的坑。

### 6. 删除文档

```http
DELETE /mall_product_basic_v1/_doc/1001?refresh=wait_for
```

删除成功返回 `result: deleted`；再次删除可能返回 `result: not_found`。删除后按 ID 查询应得到 HTTP 404 和 `found: false`。

### 7. 关键响应字段速查

| 字段 | 含义 | 判断方式 |
| --- | --- | --- |
| `acknowledged` | 集群是否确认索引级操作 | 创建、修改 Mapping、删除索引时检查 |
| `result` | 文档操作结果 | 常见值有 `created`、`updated`、`deleted`、`not_found` |
| `_version` | 当前文档内部版本 | 每次成功变更通常递增，不直接替代业务版本 |
| `found` | 按 ID 查询是否找到文档 | `false` 通常同时对应 HTTP 404 |
| `errors` | Bulk 是否至少有一个子操作失败 | 必须继续检查具体 `items` |

## 十一、常见错误与生产注意事项

### 1. 常见错误

| 现象 | 常见原因 | 处理思路 |
| --- | --- | --- |
| 连接 9200 失败 | Elasticsearch 未启动或端口被修改 | 查看进程、日志和 `http.port` |
| 创建索引返回 400 | 索引已存在或 Mapping JSON 错误 | 阅读响应中的 `error.type` 和 `reason` |
| 写入未知字段返回 400 | 本例使用 `dynamic: strict` | 修正字段名或显式新增 Mapping |
| 字段类型冲突 | 同一字段写入了不兼容类型 | 修正数据；必要时新建索引迁移 |
| 写入后搜索不到 | 尚未 Refresh | 实验用 `refresh=wait_for`，生产权衡实时性和吞吐量 |
| Bulk HTTP 200 但数据不完整 | 某些子操作失败 | 检查 `errors` 和每个 `items.*.error` |
| 单节点集群为 yellow | 配置了无法分配的副本 | 学习环境设副本为 0，生产增加节点而非简单取消容错 |

### 2. 生产实践

1. **版本兼容**：后续接入 Spring Boot 时，单独核对 Spring Data Elasticsearch、Java 客户端和服务端版本，不能凭感觉混用 7.x 与 8.x 客户端。
2. **显式 Mapping**：核心业务索引应提前设计字段类型、Analyzer 和是否允许动态字段。
3. **合理分片**：结合数据规模、节点数量、单分片大小和查询模式规划，不要提前创建大量小分片。
4. **数据一致性**：MySQL 事务成功后通过可靠事件同步 Elasticsearch，并设计重试、幂等、死信和定时对账。
5. **批量写入**：控制单批数据量和请求体大小，检查部分失败，避免无限重试拖垮集群。
6. **可观测性**：关注集群健康、未分配分片、磁盘水位、JVM、查询延迟、拒绝次数和慢查询。
7. **安全**：生产环境启用认证和 TLS，不把用户名、密码或 API Key 提交到 `.http` 文件。
8. **删除保护**：使用明确索引名和权限边界，不执行 `DELETE /*` 之类的危险操作。

## 十二、面试表达与阶段检查

### 1. 高频问题：什么是 Elasticsearch

> Elasticsearch 是基于 Lucene 构建的分布式搜索与分析引擎，以 JSON 文档为数据模型，通过倒排索引支持全文检索，并通过分片和副本实现数据分布、并行查询与容错。它适合商品搜索、日志检索和聚合分析，但不应替代 MySQL 承担订单、库存等依赖事务的最终事实数据。

### 2. 高频问题：倒排索引是什么

> 倒排索引维护词项到文档的映射。写入文本时先经过 Analyzer 处理成词项，查询时再用查询词项快速定位候选文档，因此不需要扫描所有原始文档。它是全文检索高效的核心基础。

### 3. 高频问题：为什么搜索是近实时的

> 文档写入成功后，新数据还要经过 Refresh 才会进入可搜索的 Segment。按 ID 获取文档可以实时读取，但普通搜索通常存在短暂可见性延迟。频繁强制 Refresh 会损害写入吞吐，因此需要在实时性和性能之间权衡。

### 4. 阶段验收清单

完成 [REST CRUD 请求文件](../http/elasticsearch-basic-crud.http) 后，应能独立回答和操作：

1. 解释 Index、Document、Field 和 Mapping；
2. 解释 Cluster、Node、Primary Shard 和 Replica Shard；
3. 说明 `text` 与 `keyword` 的区别；
4. 说明倒排索引和近实时搜索；
5. 创建、查看、修改 Mapping 和删除索引；
6. 创建、查询、全量写入、局部更新和删除文档；
7. 使用 Bulk 写入并检查部分失败；
8. 说明为什么商城仍以 MySQL 作为业务事实源。

