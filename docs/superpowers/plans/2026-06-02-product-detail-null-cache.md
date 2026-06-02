# Product Detail Null Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为不存在的商品详情增加 Redis 空值缓存，避免相同无效商品 ID 持续穿透到 MySQL。

**Architecture:** 商品详情缓存服务使用固定字符串保存空值标记，并为该标记配置独立的短 TTL。商品目录查询服务在普通缓存未命中后识别空值标记，命中时直接返回商品不存在；首次数据库查询确认不存在时写入空值标记。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、Redisson、JUnit 5、Mockito、MockMvc

---

### Task 1: 空值缓存回归测试

**Files:**
- Modify: `mall-backend/mall-app/src/test/java/com/tuzki/mall/product/ProductApiIntegrationTest.java`
- Create: `mall-backend/mall-product/src/test/java/com/tuzki/mall/product/service/impl/ProductCatalogServiceImplTest.java`

- [x] 增加集成测试，验证不存在商品查询后 Redis 中存在空值标记。
- [x] 在集成测试中写入同 ID 商品后再次查询，验证空值标记命中时不再查询商品表。
- [x] 运行测试并确认因空值缓存能力尚未实现而失败。

### Task 2: 空值缓存实现

**Files:**
- Modify: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/config/ProductCacheProperties.java`
- Modify: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/ProductDetailCacheService.java`
- Modify: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/impl/ProductCatalogServiceImpl.java`
- Modify: `mall-backend/mall-app/src/main/resources/application.yml`

- [x] 增加空值缓存 TTL 配置，默认 5 分钟。
- [x] 增加空值标记写入和识别方法。
- [x] 商品不存在时写入空值缓存，空值命中时直接返回 404。
- [x] 运行定向回归测试并确认通过。

### Task 3: 设计文档同步

**Files:**
- Modify: `docs/architecture/推荐商品列表与商品详情缓存设计.md`

- [x] 在缓存策略中补充空值缓存流程和短 TTL。
- [x] 在后续扩展中记录布隆过滤器，用于拦截大量随机无效商品 ID。
- [x] 运行完整后端测试并检查最终差异。
