# Product Search Index Outbox RabbitMQ Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将后台商品变更后的 Elasticsearch 同步从事务后直接调用改造成 Outbox + RabbitMQ 异步同步链路。

**Architecture:** `mall-admin` 定义商品搜索索引变更事件、事件类型和发送接口；`mall-app` 复用现有 `OutboxMessageService` 实现事件发送、RabbitMQ 拓扑和消费者；消费者最终调用已有 `ProductSearchIndexService` 完成 ES 单商品同步或删除。手动重建接口继续直接调用 `ProductSearchIndexService`，用于历史数据初始化和人工补偿。

**Tech Stack:** Spring Boot、RabbitMQ、Spring AMQP、MyBatis-Plus、Elasticsearch REST Client、Outbox 本地消息表。

**Spec:** Chat design approved by user on 2026-08-27.

## Global Constraints

- 不提交代码，由用户自行提交。
- 直接在当前分支和当前工作区修改。
- Java 类和接口新增时提供中文 JavaDoc。
- 新功能按 TDD：先写失败测试，再实现最小代码。
- 复用现有 `OutboxMessageService`，不新增第二套 Outbox 表或投递机制。
- ES 同步操作保持幂等：`SYNC` 使用 `syncProduct(productId)`，`DELETE` 使用 `deleteProduct(productId)`。

---

### Task 1: 定义商品搜索索引变更事件模型

**Files:**
- Create: `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/search/ProductSearchIndexChangedEvent.java`
- Create: `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/search/ProductSearchIndexChangedEventType.java`
- Create: `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/search/ProductSearchIndexEventSender.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/mq/ProductSearchIndexOutboxSenderTest.java`

**Interfaces:**
- Produces: `ProductSearchIndexChangedEvent(Long productId, ProductSearchIndexChangedEventType eventType)`
- Produces: `ProductSearchIndexChangedEventType.SYNC`
- Produces: `ProductSearchIndexChangedEventType.DELETE`
- Produces: `ProductSearchIndexEventSender#send(ProductSearchIndexChangedEvent event)`

- [ ] **Step 1: Write the failing test**
- [ ] **Step 2: Run test to verify it fails because event sender does not exist**
- [ ] **Step 3: Implement event record, enum, and interface**
- [ ] **Step 4: Run test to verify it passes after sender implementation in Task 2**

### Task 2: 实现 Outbox 商品搜索索引事件发送器

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/admin/mq/ProductSearchIndexOutboxSender.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/ProductSearchIndexRabbitProperties.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/mq/ProductSearchIndexOutboxSenderTest.java`

**Interfaces:**
- Consumes: `OutboxMessageService#createAndPublish(String aggregateType, String aggregateId, String exchangeName, String routingKey, Object payload)`
- Consumes: `ProductSearchIndexEventSender#send(ProductSearchIndexChangedEvent event)`
- Produces: aggregateType `"PRODUCT_SEARCH_INDEX"`
- Produces: aggregateId `"productId:eventType"`

- [ ] **Step 1: Write sender test verifying Outbox destination and aggregate fields**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement sender with configured exchange and routing key**
- [ ] **Step 4: Run sender test to verify it passes**

### Task 3: 声明 RabbitMQ 拓扑

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/ProductSearchIndexRabbitConfig.java`
- Modify: `mall-backend/mall-app/src/main/resources/application.yml`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/config/rabbit/ProductSearchIndexRabbitConfigTest.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/config/rabbit/RabbitPropertiesValidationTest.java`

**Interfaces:**
- Produces: event exchange `mall.product.search-index.event.exchange`
- Produces: event queue `mall.product.search-index.event.queue`
- Produces: event routing key `mall.product.search-index.event.routing-key`
- Produces: failed exchange `mall.product.search-index.failed.exchange`
- Produces: failed queue `mall.product.search-index.failed.queue`
- Produces: failed routing key `mall.product.search-index.failed.routing-key`

- [ ] **Step 1: Write Rabbit config test for exchange, queue, binding, and dead-letter arguments**
- [ ] **Step 2: Run test to verify it fails**
- [ ] **Step 3: Implement properties and config using existing Cart/ProductHot style**
- [ ] **Step 4: Run config and validation tests**

### Task 4: 实现 RabbitMQ 消费者

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/admin/mq/ProductSearchIndexConsumer.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/mq/ProductSearchIndexConsumerTest.java`

**Interfaces:**
- Consumes: `ProductSearchIndexChangedEvent`
- Consumes: `ProductSearchIndexService`
- Produces: `SYNC -> productSearchIndexService.syncProduct(productId)`
- Produces: `DELETE -> productSearchIndexService.deleteProduct(productId)`

- [ ] **Step 1: Write consumer tests for SYNC and DELETE**
- [ ] **Step 2: Run tests to verify they fail**
- [ ] **Step 3: Implement consumer with `@RabbitListener`**
- [ ] **Step 4: Run consumer tests**

### Task 5: 将事务后同步改为发送异步事件

**Files:**
- Modify: `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/search/ProductSearchIndexSynchronizationService.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/ProductSearchIndexSynchronizationServiceTest.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/admin/AdminProductApiIntegrationTest.java`

**Interfaces:**
- Consumes: `ProductSearchIndexEventSender#send(ProductSearchIndexChangedEvent event)`
- Produces: create/update/status -> `SYNC`
- Produces: delete -> `DELETE`

- [ ] **Step 1: Update synchronization service tests to verify event sender usage**
- [ ] **Step 2: Run tests to verify they fail**
- [ ] **Step 3: Change synchronization service from direct ES service calls to event sender calls**
- [ ] **Step 4: Run related admin integration tests**

### Task 6: Verification

**Files:**
- Related backend tests
- `docs/http/elasticsearch-product-sync.http`

**Interfaces:**
- No new production interface.

- [ ] **Step 1: Run focused Maven tests for new and affected classes**
- [ ] **Step 2: Run `git diff --check`**
- [ ] **Step 3: Report exact verification commands and results**
