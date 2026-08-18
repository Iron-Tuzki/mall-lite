# Elasticsearch 分词器与中文搜索阶段设计

## 一、目标

本阶段在 Query DSL 基础查询之后继续推进，目标是让学习者理解 analyzer、倒排索引、写入分词、查询分词、`text` 与 `keyword` 的区别，并能通过 REST API 观察中文搜索的实际行为。

## 二、范围

### 1. 包含内容

1. 新增分词器与中文搜索学习笔记。
2. 新增可执行 `.http` 练习文件，使用独立索引 `mall_product_analyzer_v1`。
3. 新增 Bulk 初始化数据文件，覆盖普通中文名称、带空格中文名称、品牌、描述等场景。
4. 提供 IK 分词器可选示例，但默认练习不依赖 IK 插件。

### 2. 不包含内容

1. 不安装 IK 插件。
2. 不修改 Elasticsearch 本地配置。
3. 不接入 Spring Boot。
4. 不提交 Git commit，由用户自行提交。

## 三、文件设计

1. `docs/notes/elasticsearch-analyzer-chinese-search.md`
   - 解释 analyzer、倒排索引、中文搜索问题、IK 分词器理解和字段设计建议。
2. `docs/http/elasticsearch-analyzer-chinese-search.http`
   - 提供 `_analyze`、创建索引、Bulk 写入、`match`、`term`、`.keyword`、`_explain` 等练习请求。
3. `docs/http/elasticsearch-analyzer-products.ndjson`
   - 提供本阶段 Bulk 初始化商品数据，文件末尾保留换行。

## 四、验证标准

1. Markdown 文档具备清晰层级和中文编号。
2. `.http` 文件只操作 `mall_product_analyzer_v1`，删除索引不使用通配符。
3. Bulk 数据文件最后一个字节必须是换行符 `10`。
4. 本地 Elasticsearch 能执行创建索引、Bulk 写入、`_analyze` 和代表性查询。
