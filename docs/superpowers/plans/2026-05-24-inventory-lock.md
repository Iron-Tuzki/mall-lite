# Inventory Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a MySQL conditional-update based stock locking capability for order creation.

**Architecture:** Keep admin inventory CRUD unchanged. Add `InventoryService.lockStock(Long skuId, Integer quantity)` as an internal business method and implement it with a single conditional update in `InventoryMapper` to avoid overselling under concurrency.

**Tech Stack:** Spring Boot 3.5.13, Java 21, MyBatis-Plus, MySQL 8.0, JUnit 5.

---

### Task 1: Inventory Lock Service

**Files:**
- Modify: `mall-backend/mall-app/src/test/java/com/tuzki/mall/inventory/InventoryApiIntegrationTest.java`
- Modify: `mall-backend/mall-inventory/src/main/java/com/tuzki/mall/inventory/mapper/InventoryMapper.java`
- Modify: `mall-backend/mall-inventory/src/main/java/com/tuzki/mall/inventory/service/InventoryService.java`
- Modify: `mall-backend/mall-inventory/src/main/java/com/tuzki/mall/inventory/service/impl/InventoryServiceImpl.java`

- [x] **Step 1: Write failing tests**

Add tests that call `InventoryService.lockStock(...)` directly:

```java
inventoryService.lockStock(skuId, 3);
```

Verify stock changes from available `10`, locked `0` to available `7`, locked `3`.

- [x] **Step 2: Verify red**

Run:

```powershell
.\scripts\dev-env.ps1; mvn -f pom.xml -pl mall-backend/mall-app -am -Dtest=InventoryApiIntegrationTest '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: compilation failure because `InventoryService.lockStock` does not exist yet.

- [x] **Step 3: Implement minimal production code**

Add `lockStock(Long skuId, Integer quantity)` to `InventoryService`, add conditional update SQL to `InventoryMapper`, and implement business validation in `InventoryServiceImpl`.

- [x] **Step 4: Verify green**

Run the same targeted test command. Expected: `InventoryApiIntegrationTest` passes.

- [x] **Step 5: Full verification**

Run:

```powershell
.\scripts\dev-env.ps1; mvn -f pom.xml test
```

Expected: full project build success with no test failures.
