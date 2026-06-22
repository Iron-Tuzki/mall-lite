# Elasticsearch Basic Concepts and REST CRUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成 Elasticsearch 7.17.23 基础知识文档和可执行的 IntelliJ HTTP Client 请求文件，并在本机完成索引与文档 CRUD 实测。

**Architecture:** 知识说明与可执行实验分成两个文件：`docs/notes` 保存稳定的概念、原理和生产注意事项，`docs/http` 保存只操作独立练习索引的 REST 请求。验证分为 Markdown/HTTP 静态检查和 `localhost:9200` 实际请求检查，实测结束后删除练习索引。

**Tech Stack:** Elasticsearch 7.17.23、Kibana 7.17.23、Elasticsearch REST API、IntelliJ IDEA HTTP Client、Markdown、PowerShell

---

## 一、文件结构

- Create: `docs/notes/elasticsearch-basic-concepts.md`：基础概念、运行说明、REST CRUD 语义、常见误区、生产实践和面试表达。
- Create: `docs/http/elasticsearch-basic-crud.http`：可按顺序执行并可重复清理的索引与文档 CRUD 请求。
- Reference: `docs/superpowers/specs/2026-06-22-elasticsearch-basic-crud-design.md`：已确认的范围、字段模型和验收标准。

## 二、实施任务

### Task 1: 编写 Elasticsearch 基础知识文档

**Files:**
- Create: `docs/notes/elasticsearch-basic-concepts.md`
- Reference: `docs/superpowers/specs/2026-06-22-elasticsearch-basic-crud-design.md`

- [ ] **Step 1: 创建文档骨架并写明版本边界**

文档必须以以下层级组织：

```markdown
# Elasticsearch 基础概念与 REST CRUD

> 本文基于 Elasticsearch 7.17.23 和 Kibana 7.17.23。

## 一、Elasticsearch 解决什么问题
## 二、Elasticsearch、Lucene 与 Kibana
## 三、核心数据模型
## 四、集群、节点、分片与副本
## 五、Mapping 与字段类型
## 六、倒排索引、分词与近实时搜索
## 七、Elasticsearch 与 MySQL 的区别
## 八、本地环境启动与验证
## 九、索引 CRUD
## 十、文档 CRUD
## 十一、常见错误与生产注意事项
## 十二、面试表达与阶段检查
```

- [ ] **Step 2: 写清核心概念与心智模型**

覆盖以下定义及关系，不将 Elasticsearch 描述为 MySQL 的替代品：

1. Elasticsearch 是面向搜索和分析的分布式文档数据库，底层使用 Lucene。
2. Kibana 是查询、可视化和运维界面，不负责保存业务索引数据。
3. Index 类似文档集合，Document 是 JSON 数据，Field 是文档字段，Mapping 是字段结构与索引方式定义。
4. Cluster 由 Node 组成，Index 被拆成 Primary Shard，Replica Shard 提供容错和部分读取扩展能力。
5. 倒排索引保存“词项到文档”的映射；Analyzer 按字符过滤、分词和词项过滤流程处理文本。
6. `text` 用于全文检索，`keyword` 用于精确匹配、排序和聚合。
7. Elasticsearch 的搜索是近实时的，写入成功不等于普通搜索请求立即可见。

- [ ] **Step 3: 加入 REST CRUD 语义和响应判断**

文档中的请求路径必须与实验文件一致，并解释：

```text
PUT    /mall_product_basic_v1
GET    /mall_product_basic_v1
PUT    /mall_product_basic_v1/_mapping
DELETE /mall_product_basic_v1
PUT    /mall_product_basic_v1/_doc/1001
POST   /mall_product_basic_v1/_doc
GET    /mall_product_basic_v1/_doc/1001
PUT    /mall_product_basic_v1/_doc/1001
POST   /mall_product_basic_v1/_update/1001
DELETE /mall_product_basic_v1/_doc/1001
POST   /_bulk
```

说明 `acknowledged`、`result`、`_version`、`found`、`errors` 和 HTTP 状态码的判断方式，强调 `PUT _doc/{id}` 是完整文档写入而 `_update` 是局部更新。

- [ ] **Step 4: 加入生产实践和面试表达**

至少覆盖：显式 Mapping、字段类型不可原地修改、分片数量不能随意堆高、MySQL 作为业务事实源、Elasticsearch 作为搜索副本、不要使用通配符删除生产索引、不要在请求文件保存密钥。

- [ ] **Step 5: 静态验证 Markdown**

Run:

```powershell
rg -n '^# |^## [一二三四五六七八九十]+、' docs/notes/elasticsearch-basic-concepts.md
rg -n 'Elasticsearch 7\.17\.23|Lucene|Kibana|Index|Document|Mapping|Shard|Replica|倒排索引|近实时|MySQL|mall_product_basic_v1' docs/notes/elasticsearch-basic-concepts.md
rg -n 'TBD|TODO|待补充|以后再写' docs/notes/elasticsearch-basic-concepts.md
```

Expected:

1. 第一条输出一个一级标题和十二个中文序号二级标题。
2. 第二条列出的每个主题至少出现一次。
3. 第三条无输出，退出码为 1 表示没有占位内容。

- [ ] **Step 6: 提交知识文档**

```powershell
git add -- docs/notes/elasticsearch-basic-concepts.md
git commit -m "docs: 添加 Elasticsearch 基础概念笔记"
```

### Task 2: 创建可执行 REST CRUD 请求文件

**Files:**
- Create: `docs/http/elasticsearch-basic-crud.http`
- Reference: `docs/notes/elasticsearch-basic-concepts.md`

- [ ] **Step 1: 定义环境变量与健康检查请求**

文件开头使用：

```http
@esHost = http://localhost:9200
@indexName = mall_product_basic_v1

GET {{esHost}}/

###

GET {{esHost}}/_cluster/health
```

- [ ] **Step 2: 编写可重复执行的索引 CRUD**

按以下顺序提供请求并添加中文注释：

1. `DELETE /{{indexName}}?ignore_unavailable=true` 清理残留索引。
2. `PUT /{{indexName}}` 使用显式 Mapping 创建索引。
3. `GET /{{indexName}}` 查询索引定义。
4. `GET /{{indexName}}/_mapping` 查询 Mapping。
5. `PUT /{{indexName}}/_mapping` 新增 `brandName` keyword 字段。
6. 最后一组请求使用 `DELETE /{{indexName}}` 删除索引并用 `HEAD /{{indexName}}` 验证不存在。

创建索引请求体必须包含：

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "productId": { "type": "long" },
      "productName": { "type": "text" },
      "productCode": { "type": "keyword" },
      "categoryId": { "type": "long" },
      "price": { "type": "scaled_float", "scaling_factor": 100 },
      "status": { "type": "keyword" },
      "tags": { "type": "keyword" },
      "description": { "type": "text" },
      "createdAt": { "type": "date", "format": "strict_date_optional_time||epoch_millis" }
    }
  }
}
```

- [ ] **Step 3: 编写文档 CRUD 请求**

使用固定商品 ID `1001` 演示：

1. `PUT /{{indexName}}/_doc/1001?refresh=wait_for` 创建文档。
2. `POST /{{indexName}}/_doc?refresh=wait_for` 使用自动 ID 创建文档。
3. `GET /{{indexName}}/_doc/1001` 按 ID 查询。
4. `PUT /{{indexName}}/_doc/1001?refresh=wait_for` 全量替换，并在请求体中保留 Mapping 需要的全部业务字段。
5. `POST /{{indexName}}/_update/1001?refresh=wait_for` 通过 `doc` 局部修改 `price` 和 `status`。
6. `DELETE /{{indexName}}/_doc/1001?refresh=wait_for` 删除文档。
7. 再次查询 ID `1001`，预期返回 HTTP 404 和 `found: false`。

- [ ] **Step 4: 编写 Bulk 批量请求与结果查询**

Bulk 请求使用 NDJSON，每个动作行和数据行各占一行，文件末尾保留换行：

```http
POST {{esHost}}/_bulk?refresh=wait_for
Content-Type: application/x-ndjson

{"index":{"_index":"mall_product_basic_v1","_id":"1002"}}
{"productId":1002,"productName":"机械键盘","productCode":"KEYBOARD-1002","categoryId":20,"price":399.00,"status":"ON_SALE","tags":["外设","键盘"],"description":"支持热插拔的机械键盘","createdAt":"2026-06-22T10:00:00+08:00","brandName":"Mall Lite"}
{"index":{"_index":"mall_product_basic_v1","_id":"1003"}}
{"productId":1003,"productName":"无线鼠标","productCode":"MOUSE-1003","categoryId":20,"price":129.00,"status":"ON_SALE","tags":["外设","鼠标"],"description":"轻量无线鼠标","createdAt":"2026-06-22T10:05:00+08:00","brandName":"Mall Lite"}
```

随后执行：

```http
GET {{esHost}}/{{indexName}}/_search
Content-Type: application/json

{
  "query": { "match_all": {} },
  "sort": [{ "productId": "asc" }]
}
```

- [ ] **Step 5: 静态验证 HTTP 文件**

Run:

```powershell
rg -n '^@esHost|^@indexName|^(GET|PUT|POST|DELETE|HEAD) ' docs/http/elasticsearch-basic-crud.http
rg -n 'ignore_unavailable=true|dynamic.*strict|scaled_float|scaling_factor|refresh=wait_for|application/x-ndjson|_bulk|_update|_mapping' docs/http/elasticsearch-basic-crud.http
```

Expected: 输出包含两个变量，以及健康检查、索引 CRUD、文档 CRUD、Bulk 和最终清理请求；所有必需的安全与可见性参数均存在。

- [ ] **Step 6: 提交 REST 请求文件**

```powershell
git add -- docs/http/elasticsearch-basic-crud.http
git commit -m "docs: 添加 Elasticsearch REST CRUD 实验"
```

### Task 3: 在本机 Elasticsearch 执行端到端验证

**Files:**
- Verify: `docs/http/elasticsearch-basic-crud.http`
- Verify: `docs/notes/elasticsearch-basic-concepts.md`

- [ ] **Step 1: 检查服务版本和集群状态**

Run:

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:9200/'
Invoke-RestMethod -Method Get -Uri 'http://localhost:9200/_cluster/health'
```

Expected: 根请求返回 `version.number` 为 `7.17.23`；集群状态为 `green` 或单节点环境可解释的 `yellow`。

- [ ] **Step 2: 按 `.http` 文件顺序执行索引与文档请求**

逐条执行并检查：

1. 创建索引得到 `acknowledged: true`。
2. 创建固定 ID 文档得到 `result: created`。
3. 全量替换得到 `result: updated` 且 `_version` 增加。
4. 局部更新得到 `result: updated`。
5. Bulk 顶层 `errors` 为 `false`。
6. 删除固定 ID 文档得到 `result: deleted`。
7. 删除后查询返回 HTTP 404 和 `found: false`。

- [ ] **Step 3: 清理练习索引**

Run:

```powershell
Invoke-RestMethod -Method Delete -Uri 'http://localhost:9200/mall_product_basic_v1'
```

Expected: 返回 `acknowledged: true`。

- [ ] **Step 4: 验证索引已清理**

Run:

```powershell
try {
    Invoke-WebRequest -Method Head -Uri 'http://localhost:9200/mall_product_basic_v1'
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

Expected: 输出 `404`。

### Task 4: 最终一致性检查

**Files:**
- Verify: `docs/notes/elasticsearch-basic-concepts.md`
- Verify: `docs/http/elasticsearch-basic-crud.http`
- Reference: `docs/superpowers/specs/2026-06-22-elasticsearch-basic-crud-design.md`

- [ ] **Step 1: 检查规格覆盖和文件差异**

Run:

```powershell
git diff --check HEAD~2..HEAD
git status --short
```

Expected: `git diff --check` 无输出；状态只允许保留用户原有的未跟踪 IDE 文件，不应出现本阶段未提交文件。

- [ ] **Step 2: 对照规格完成最终审阅**

确认：

1. 知识文档包含规格要求的全部基础主题。
2. `.http` 文件只操作 `mall_product_basic_v1`，没有通配符删除。
3. 文档示例与请求文件的路径、字段、版本一致。
4. 实测已经清理练习索引。
5. 未修改 `C:\MySoftware` 下的任何文件。

