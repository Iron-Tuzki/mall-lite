# Maven Multi Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first Maven multi-module Spring Boot skeleton for `mall-lite`.

**Architecture:** Use `mall-backend` as the Maven aggregator parent. Keep business modules as plain jar modules, and use `mall-app` as the only Spring Boot executable module that depends on the business modules.

**Tech Stack:** JDK 21, Spring Boot 3.5.13, Maven multi-module, MyBatis-Plus 3.5.x, MySQL 8.0, Redis 5.0.14.1, RabbitMQ 3.12.0.

---

### Task 1: Parent POM

**Files:**
- Create: `mall-backend/pom.xml`

- [ ] **Step 1: Create the aggregator parent POM**

Create a parent POM with packaging `pom`, Java 21 compiler settings, Spring Boot 3.5.13 dependency management, and all seven modules.

- [ ] **Step 2: Verify XML structure**

Run: `Get-Content -Encoding UTF8 mall-backend\pom.xml`

Expected: the file contains `<packaging>pom</packaging>` and all modules from `mall-common` to `mall-app`.

### Task 2: Module POMs

**Files:**
- Create: `mall-backend/mall-common/pom.xml`
- Create: `mall-backend/mall-user/pom.xml`
- Create: `mall-backend/mall-product/pom.xml`
- Create: `mall-backend/mall-inventory/pom.xml`
- Create: `mall-backend/mall-order/pom.xml`
- Create: `mall-backend/mall-payment/pom.xml`
- Create: `mall-backend/mall-app/pom.xml`

- [ ] **Step 1: Create jar module POMs**

Each business module inherits from `mall-backend` and uses packaging `jar`.

- [ ] **Step 2: Create app module POM**

`mall-app` inherits from `mall-backend`, depends on all business modules, and declares Spring Boot Web, Validation, MyBatis-Plus, MySQL, Redis, and AMQP starters.

- [ ] **Step 3: Verify module dependencies**

Run: `Select-String -Path mall-backend\mall-app\pom.xml -Pattern "mall-user|mall-product|mall-inventory|mall-order|mall-payment|spring-boot-starter-web"`

Expected: all business modules and Spring Web dependency are present.

### Task 3: Source Directories And Startup Class

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/MallLiteApplication.java`
- Create: `mall-backend/mall-app/src/main/resources/application.yml`
- Create: package marker files for each business module.

- [ ] **Step 1: Create standard Maven source folders**

Create `src/main/java`, `src/main/resources`, and `src/test/java` folders where useful.

- [ ] **Step 2: Create Spring Boot startup class**

Add `MallLiteApplication` under package `com.tuzki.mall` with JavaDoc explaining the class purpose.

- [ ] **Step 3: Create initial application configuration**

Add `application.yml` with application name, server port, datasource, Redis, RabbitMQ, and MyBatis-Plus starter configuration placeholders suitable for local development.

### Task 4: Verification

**Files:**
- Read: `mall-backend/pom.xml`
- Read: `mall-backend/mall-app/pom.xml`
- Read: `mall-backend/mall-app/src/main/java/com/tuzki/mall/MallLiteApplication.java`

- [ ] **Step 1: Check Maven availability**

Run: `mvn -version`

Expected: if Maven is installed, print Maven and Java version; if not installed, record that Maven CLI verification is blocked.

- [ ] **Step 2: Run Maven validation when available**

Run: `mvn -f mall-backend\pom.xml validate`

Expected: build success after dependencies are available.

- [ ] **Step 3: Perform file-level verification**

Run: `Get-ChildItem -Recurse -Filter pom.xml mall-backend`

Expected: one parent POM and seven module POMs are present.
