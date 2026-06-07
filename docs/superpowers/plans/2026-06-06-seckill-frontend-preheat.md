# Seckill Frontend And Preheat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a customer-facing seckill page, automatic Redis preheat scheduling, and sample seckill data.

**Architecture:** Keep seckill orchestration in `mall-seckill`, add the app-layer scheduled task beside existing scheduling jobs, and add a focused Vue page that calls the existing seckill APIs directly. Reuse login, address, and order-result flows.

**Tech Stack:** Spring Boot scheduling, MyBatis-Plus, Redisson, Vue 3, Vite, Element Plus.

---

### Task 1: Backend Preheat Scheduling

**Files:**
- Modify: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/SeckillService.java`
- Modify: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/impl/SeckillServiceImpl.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/scheduling/SeckillPreheatTask.java`
- Modify: `mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java`

- [ ] Add a failing integration test that creates current, soon-starting, future, disabled, and ended activities, invokes the task, and verifies only current and soon-starting activities are preheated.
- [ ] Run the seckill integration test and confirm it fails because the task/query method does not exist.
- [ ] Add `listPreheatableActivityIds(Duration preheatWindow)` to `SeckillService`.
- [ ] Implement the query as `status = 1`, `deleted = 0`, `startTime <= now + window`, `endTime > now`.
- [ ] Add `SeckillPreheatTask` with `@Scheduled`, `@RedisDistributedLock`, enable flag, fixed delay, and window minutes properties.
- [ ] Run the seckill integration test and confirm it passes.

### Task 2: SQL Seed And Ignore

**Files:**
- Modify: `scripts/mysql/015_seckill_schema.sql`
- Modify: `.gitignore`

- [ ] Add `/mall-backend/mall-seckill/target/` to `.gitignore`.
- [ ] Add demo seckill activity and SKU inserts using existing `pms_sku` rows with scalar subqueries, so seed data follows local SKU IDs.
- [ ] Keep SQL idempotent with explicit delete-by-name before insert.

### Task 3: Frontend Seckill Page

**Files:**
- Create: `mall-frontend/src/api/seckill.ts`
- Create: `mall-frontend/src/views/SeckillView.vue`
- Modify: `mall-frontend/src/router/index.ts`
- Modify: `mall-frontend/src/views/HomeView.vue`

- [ ] Add typed seckill API functions.
- [ ] Add `/seckill` route requiring login only for order submission, not for browsing.
- [ ] Build a page that lists active activities and SKUs, selects quantity, loads default address on submission, creates a seckill order, and redirects to the order result page.
- [ ] Add a visible homepage entry that links to `/seckill`.
- [ ] Run `npm run build`.

### Task 4: Verification

- [ ] Run `mvn -f mall-backend/pom.xml -pl mall-app -am '-Dtest=SeckillApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- [ ] Run `mvn -f mall-backend/pom.xml test`.
- [ ] Run `npm run build` in `mall-frontend`.
- [ ] Check `git status --short` and report changes without staging or committing.
