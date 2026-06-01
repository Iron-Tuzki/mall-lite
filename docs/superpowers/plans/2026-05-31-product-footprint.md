# 商品浏览足迹 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 Redis ZSet 实现登录用户最近 100 条商品浏览足迹，并接入个人主页和完整足迹页。

**Architecture:** `mall-product` 负责 Redis ZSet 写入、查询和清理，并批量读取最新商品信息。`mall-app` 提供登录用户足迹管理接口，并在商品详情查询成功后尽力记录足迹。Vue 前端用真实接口替换个人主页静态数据，并增加完整足迹页。

**Tech Stack:** Spring Boot 3.5、Redisson、MyBatis-Plus、Vue 3、Element Plus

---

### Task 1: Redis ZSet 足迹服务

**Files:**
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/ProductFootprintService.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/impl/ProductFootprintServiceImpl.java`
- Create: `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/vo/ProductFootprintVO.java`
- Create: `mall-backend/mall-product/src/test/java/com/tuzki/mall/product/service/impl/ProductFootprintServiceImplTest.java`

- [ ] 编写失败测试：首次浏览、重复浏览更新时间、仅保留最近 100 条、倒序查询、删除单条、清空全部。
- [ ] 运行 `mvn -f pom.xml -pl mall-backend/mall-product -am -Dtest=ProductFootprintServiceImplTest '-Dsurefire.failIfNoSpecifiedTests=false' test`，确认因缺少实现失败。
- [ ] 使用 `RScoredSortedSet<String>` 实现最小功能，key 为 `mall:user:footprints:{userId}`，TTL 为 90 天。
- [ ] 重跑测试，确认通过。

### Task 2: 足迹接口与详情页写入

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/product/controller/ProductFootprintController.java`
- Modify: `mall-backend/mall-app/src/main/java/com/tuzki/mall/product/controller/ProductController.java`
- Create: `mall-backend/mall-app/src/test/java/com/tuzki/mall/product/ProductFootprintApiIntegrationTest.java`

- [ ] 编写失败测试：登录用户可查询、删除和清空足迹；匿名和失效 token 浏览详情不受影响。
- [ ] 运行单测并确认因接口缺失失败。
- [ ] 增加 `/api/product-footprints` 接口。
- [ ] 在商品详情查询成功后尝试解析 token 并写入足迹；解析失败或 Redis 异常仅记录日志。
- [ ] 重跑测试，确认通过。

### Task 3: 前端个人主页和完整足迹页

**Files:**
- Modify: `mall-frontend/src/api/product.ts`
- Modify: `mall-frontend/src/router/index.ts`
- Modify: `mall-frontend/src/views/ProfileView.vue`
- Create: `mall-frontend/src/views/FootprintProductsView.vue`

- [ ] 增加足迹列表、删除单条和清空全部 API。
- [ ] 个人主页加载最近 3 条真实足迹，并让“全部足迹”跳转 `/footprints`。
- [ ] 增加 `/footprints` 登录路由。
- [ ] 创建完整足迹页，支持查看、删除单条、清空全部和空状态。
- [ ] 运行 `npm run build`，确认构建通过。

### Task 4: 完整验证

- [ ] 运行后端足迹测试。
- [ ] 运行 `mvn -f pom.xml -pl mall-backend/mall-app -am test -DskipTests`。
- [ ] 运行前端 `npm run build`。
- [ ] 使用本地页面验证个人主页和 `/footprints` 布局。

