# Sign In Yearly Heatmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a user yearly sign-in detail page that renders 12 monthly Redis Bitmap summaries without adding MySQL sign-in tables.

**Architecture:** Backend exposes a login-protected yearly sign-in endpoint backed by the existing Redis Bitmap keys. Frontend adds a yearly heatmap API type, route, page, and entry link from the profile sign-in panel.

**Tech Stack:** Spring Boot, Redisson, JUnit, MockMvc, Vue 3, TypeScript, Element Plus, Vite.

---

### Task 1: Backend VO And Service Contract

**Files:**
- Create: `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/vo/SignInMonthProfileVO.java`
- Create: `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/vo/SignInYearlyProfileVO.java`
- Modify: `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/service/SignInService.java`

- [ ] **Step 1: Add monthly and yearly VO classes**

Create Java classes with Chinese JavaDoc. `SignInMonthProfileVO` contains `month`, `daysInMonth`, `signedCount`, and `signedDays`. `SignInYearlyProfileVO` contains `year`, `yearSignedCount`, and `months`.

- [ ] **Step 2: Add service method**

Add `SignInYearlyProfileVO getYearlyProfile(Long userId, Integer year);` to `SignInService` with JavaDoc explaining `userId` and `year`.

### Task 2: Backend Redis Aggregation

**Files:**
- Modify: `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/service/impl/RedisSignInService.java`
- Test: `mall-backend/mall-user/src/test/java/com/tuzki/mall/user/service/impl/RedisSignInServiceTest.java`

- [ ] **Step 1: Write unit tests**

Test yearly aggregation with mocked Redisson `RBitSet` objects: returns 12 months, handles leap-year February, counts signed days, and rejects invalid years.

- [ ] **Step 2: Implement yearly aggregation**

Resolve year from parameter or `LocalDate.now(BUSINESS_ZONE)`, validate range, loop through `YearMonth.of(year, month)`, read each month Bitmap, and build monthly summaries.

- [ ] **Step 3: Run test**

Run `mvn -f mall-backend/pom.xml -pl mall-user test`.

### Task 3: Backend API

**Files:**
- Modify: `mall-backend/mall-app/src/main/java/com/tuzki/mall/user/controller/UserController.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/user/UserApiIntegrationTest.java`

- [ ] **Step 1: Add controller endpoint**

Add `GET /api/users/sign-in/yearly` with optional `year` request parameter and existing token resolution.

- [ ] **Step 2: Add API integration assertions**

Add a missing-token test for the yearly endpoint and a logged-in response shape test that verifies `year`, 12 months, and first-month fields.

- [ ] **Step 3: Run API test**

Run `mvn -f mall-backend/pom.xml -pl mall-app -Dtest=UserApiIntegrationTest test`.

### Task 4: Frontend API And Route

**Files:**
- Modify: `mall-frontend/src/api/user.ts`
- Modify: `mall-frontend/src/router/index.ts`
- Modify: `mall-frontend/src/views/ProfileView.vue`
- Create: `mall-frontend/src/views/SignInYearlyView.vue`

- [ ] **Step 1: Add frontend types and API method**

Add `SignInMonthProfile`, `SignInYearlyProfile`, and `getSignInYearlyProfile(year?: number)`.

- [ ] **Step 2: Add route**

Add `/profile/sign-in/yearly` route requiring auth.

- [ ] **Step 3: Add profile entry**

Add a small “年度详情” button in the existing sign-in section that routes to `/profile/sign-in/yearly`.

- [ ] **Step 4: Build yearly page**

Render stats and 12 month heatmaps from API data. Include loading and error feedback.

- [ ] **Step 5: Run frontend build**

Run `npm run build` in `mall-frontend`.

### Task 5: Final Verification

**Files:**
- Review all changed files.

- [ ] **Step 1: Run focused backend tests**

Run `mvn -f mall-backend/pom.xml -pl mall-user,mall-app test`.

- [ ] **Step 2: Run frontend build**

Run `npm run build` in `mall-frontend`.

- [ ] **Step 3: Summarize**

Report changed files, verification result, and note that no MySQL migration was added.
