# Admin Product Seckill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an admin management module for platform products, SKUs, inventory, seckill activities, and seckill SKU selection.

**Architecture:** Add a focused `mall-admin` backend module that orchestrates existing product, inventory, and seckill tables. Keep Web controllers in `mall-app` to match the current project style, and add admin routes inside the existing Vue frontend without a new dev port.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven multi-module, MyBatis-Plus, Vue 3, TypeScript, Vite, Element Plus.

---

## File Structure

Create backend module files:

- `mall-backend/mall-admin/pom.xml`: admin module dependencies.
- `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/package-info.java`: module documentation.
- `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/**`: product admin DTOs, VOs, service.
- `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/seckill/**`: seckill admin DTOs, VOs, service.
- `mall-backend/mall-app/src/main/java/com/tuzki/mall/admin/controller/**`: admin Web controllers.
- `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/**`: focused admin integration tests.

Modify backend files:

- `mall-backend/pom.xml`: register `mall-admin`.
- `mall-backend/mall-app/pom.xml`: depend on `mall-admin`.
- Product cache services if they need explicit eviction methods.

Create frontend files:

- `mall-frontend/src/api/admin/product.ts`: admin product API.
- `mall-frontend/src/api/admin/seckill.ts`: admin seckill API.
- `mall-frontend/src/views/admin/AdminLayoutView.vue`: admin shell.
- `mall-frontend/src/views/admin/AdminProductListView.vue`: product list.
- `mall-frontend/src/views/admin/AdminProductEditView.vue`: product edit.
- `mall-frontend/src/views/admin/AdminSeckillListView.vue`: seckill list.
- `mall-frontend/src/views/admin/AdminSeckillEditView.vue`: seckill edit and SKU selection.

Modify frontend files:

- `mall-frontend/src/router/index.ts`: add `/admin` route group.

## Task 1: Add mall-admin Module

- [ ] Add failing compile check: `mvn -f mall-backend/pom.xml -pl mall-admin -am test -DskipTests`, expected failure because module is not registered.
- [ ] Create `mall-admin/pom.xml` with dependencies on `mall-common`, `mall-product`, `mall-inventory`, `mall-seckill`, MyBatis-Plus, validation, and Spring context.
- [ ] Add `<module>mall-admin</module>` to `mall-backend/pom.xml`.
- [ ] Add `mall-admin` dependency to `mall-app/pom.xml`.
- [ ] Add Chinese JavaDoc package documentation.
- [ ] Run `mvn -f mall-backend/pom.xml -pl mall-admin -am test -DskipTests`, expected success.
- [ ] Do not commit; leave changes in the worktree.

## Task 2: Backend Product Admin

- [ ] Write a failing integration test for creating a product with one SKU and inventory through `POST /api/admin/products`.
- [ ] Implement product admin DTOs and VOs with validation.
- [ ] Implement `AdminProductService` with transactional create, update, soft delete, status update, detail, list, and selectable SKU list.
- [ ] Implement `AdminProductController`.
- [ ] Run the focused product admin test and make it pass.
- [ ] Add tests for duplicate product code, duplicate SKU code, SKU ownership on update, and soft delete visibility.
- [ ] Do not commit; leave changes in the worktree.

## Task 3: Backend Seckill Admin

- [ ] Write a failing integration test for creating a seckill activity through `POST /api/admin/seckill/activities`.
- [ ] Implement seckill admin DTOs and VOs with validation.
- [ ] Implement `AdminSeckillService` with activity CRUD, status update, SKU add/update/delete, detail, list, and preheat delegation.
- [ ] Implement `AdminSeckillController`.
- [ ] Run the focused seckill admin test and make it pass.
- [ ] Add tests for invalid activity time, seckill price above original price, duplicate activity SKU, and soft delete.
- [ ] Do not commit; leave changes in the worktree.

## Task 4: Frontend Admin Product Pages

- [ ] Add admin product API TypeScript types and functions.
- [ ] Add admin layout and route group.
- [ ] Add product list page with filters, pagination, status toggle, delete, and edit navigation.
- [ ] Add product edit page with SPU form, SKU table, inventory field, validation, create, and update.
- [ ] Run `npm run build` in `mall-frontend`, expected success.
- [ ] Do not commit; leave changes in the worktree.

## Task 5: Frontend Admin Seckill Pages

- [ ] Add admin seckill API TypeScript types and functions.
- [ ] Add seckill list page with filters, pagination, status toggle, delete, preheat, and edit navigation.
- [ ] Add seckill edit page with activity form, activity SKU table, selectable SKU dialog, and validations.
- [ ] Run `npm run build` in `mall-frontend`, expected success.
- [ ] Do not commit; leave changes in the worktree.

## Task 6: Final Verification

- [ ] Run focused backend tests for admin product and seckill.
- [ ] Run `mvn -f mall-backend/pom.xml -pl mall-app -am test -DskipTests` at minimum to confirm compilation.
- [ ] Run `npm run build` in `mall-frontend`.
- [ ] Check `git -c safe.directory=C:/Users/Tuzki/IdeaProjects/mall-lite status --short`.
- [ ] Report changed files and verification output. Do not commit.

## Self-Review

Spec coverage:

1. `mall-admin` backend module: Task 1.
2. Product SPU, SKU, inventory CRUD: Task 2 and Task 4.
3. Seckill activity and activity SKU management: Task 3 and Task 5.
4. Existing frontend port and admin routes: Task 4 and Task 5.
5. No admin auth and no shop dimension: enforced by controller/service scope.
6. Verification without commit: Task 6.

No placeholders are intentionally left. The user explicitly requested no commits and no isolated worktree, so all commit steps are omitted.
