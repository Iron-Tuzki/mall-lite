# Seckill MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the backend MVP for seckill activities, Redis stock preheating, user purchase limits, request idempotency, and synchronous order creation with seckill price.

**Architecture:** Add a focused `mall-seckill` module for activity data, Redis pre-deduction, and seckill order orchestration. Reuse the existing order, inventory, login session, and timeout-cancel flows; extend `mall-order` only enough to support price overrides.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven multi-module, MyBatis-Plus, Redis via Redisson, MockMvc integration tests, MySQL schema scripts.

---

## File Structure

Create:

- `scripts/mysql/015_seckill_schema.sql`: seckill activity and activity SKU schema.
- `mall-backend/mall-seckill/pom.xml`: seckill module dependencies.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/package-info.java`: package documentation.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/entity/SeckillActivity.java`: activity entity.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/entity/SeckillSku.java`: activity SKU entity.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/mapper/SeckillActivityMapper.java`: activity mapper.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/mapper/SeckillSkuMapper.java`: activity SKU mapper.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/dto/SeckillOrderCreateRequest.java`: seckill order request.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/vo/SeckillActivityVO.java`: active activity response.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/vo/SeckillSkuVO.java`: active activity SKU response.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/SeckillService.java`: seckill service interface.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/impl/SeckillServiceImpl.java`: orchestration implementation.
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/redis/SeckillRedisService.java`: Redis key, preheat, Lua pre-deduction, compensation.
- `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/controller/SeckillController.java`: public seckill APIs.
- `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/controller/SeckillAdminController.java`: admin preheat API.
- `mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java`: API and Redis behavior tests.

Modify:

- `mall-backend/pom.xml`: add `mall-seckill` module.
- `mall-backend/mall-app/pom.xml`: depend on `mall-seckill`.
- `mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/OrderService.java`: add price override order creation method.
- `mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/impl/OrderServiceImpl.java`: implement price override path while preserving existing order behavior.
- `mall-backend/mall-app/src/test/java/com/tuzki/mall/order/OrderCreateApiIntegrationTest.java`: add one price override service-level test or leave API tests unchanged and cover through seckill API.
- `mall-backend/mall-app/src/test/java/com/tuzki/mall/TestSeedData.java`: add seckill test constants only if needed.

Moved:

- `docs/architecture/秒杀功能后端MVP设计.md`: approved design document moved from `docs/superpowers/specs`.

---

### Task 1: Move Design Document And Add Schema

**Files:**
- Moved: `docs/architecture/秒杀功能后端MVP设计.md`
- Create: `scripts/mysql/015_seckill_schema.sql`

- [ ] **Step 1: Verify the design document is in architecture docs**

Run:

```powershell
Test-Path 'docs\architecture\秒杀功能后端MVP设计.md'
```

Expected: `True`

- [ ] **Step 2: Add the failing schema existence check**

Run:

```powershell
Test-Path 'scripts\mysql\015_seckill_schema.sql'
```

Expected before implementation: `False`

- [ ] **Step 3: Create the schema script**

Add `scripts/mysql/015_seckill_schema.sql`:

```sql
USE mall_lite;

DROP TABLE IF EXISTS sms_seckill_sku;
DROP TABLE IF EXISTS sms_seckill_activity;

CREATE TABLE sms_seckill_activity
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
    name        VARCHAR(128)    NOT NULL COMMENT '活动名称',
    start_time  DATETIME        NOT NULL COMMENT '活动开始时间',
    end_time    DATETIME        NOT NULL COMMENT '活动结束时间',
    status      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    remark      VARCHAR(255)    NULL COMMENT '活动备注',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_time_status (start_time, end_time, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动表';

CREATE TABLE sms_seckill_sku
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动商品ID',
    activity_id    BIGINT UNSIGNED NOT NULL COMMENT '秒杀活动ID',
    sku_id         BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    seckill_price  DECIMAL(10, 2)  NOT NULL COMMENT '秒杀价',
    stock_count    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '活动库存',
    limit_quantity INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    sort           INT             NOT NULL DEFAULT 0 COMMENT '排序值',
    status         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_sku (activity_id, sku_id),
    KEY idx_activity_status_sort (activity_id, status, sort),
    KEY idx_sku_id (sku_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀活动商品表';
```

- [ ] **Step 4: Verify schema file exists**

Run:

```powershell
Test-Path 'scripts\mysql\015_seckill_schema.sql'
```

Expected: `True`

- [ ] **Step 5: Commit**

Run:

```bash
git add docs/architecture/秒杀功能后端MVP设计.md docs/superpowers/specs/2026-06-05-seckill-mvp-design.md scripts/mysql/015_seckill_schema.sql
git commit -m "docs: move seckill design and add schema"
```

Expected: commit succeeds. If the old spec path no longer exists, `git add -A docs scripts/mysql/015_seckill_schema.sql` is acceptable.

---

### Task 2: Add mall-seckill Module Skeleton

**Files:**
- Modify: `mall-backend/pom.xml`
- Modify: `mall-backend/mall-app/pom.xml`
- Create: `mall-backend/mall-seckill/pom.xml`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/package-info.java`

- [ ] **Step 1: Add a failing module build check**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-seckill -am test -DskipTests
```

Expected before implementation: FAIL because module `mall-seckill` is not found.

- [ ] **Step 2: Add module POM**

Create `mall-backend/mall-seckill/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.tuzki</groupId>
        <artifactId>mall-backend</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>mall-seckill</artifactId>
    <packaging>jar</packaging>

    <name>mall-seckill</name>
    <description>Seckill activity and order qualification module for mall-lite.</description>

    <dependencies>
        <dependency>
            <groupId>com.tuzki</groupId>
            <artifactId>mall-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.tuzki</groupId>
            <artifactId>mall-product</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.tuzki</groupId>
            <artifactId>mall-order</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Register module and app dependency**

In `mall-backend/pom.xml`, add:

```xml
<module>mall-seckill</module>
```

Place it after `mall-payment` or next to the other business modules.

In `mall-backend/mall-app/pom.xml`, add:

```xml
<dependency>
    <groupId>com.tuzki</groupId>
    <artifactId>mall-seckill</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4: Add package documentation**

Create `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/package-info.java`:

```java
/**
 * 秒杀业务模块，负责秒杀活动查询、活动库存预热、用户限购校验和秒杀下单资格控制。
 */
package com.tuzki.mall.seckill;
```

- [ ] **Step 5: Verify module compiles**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-seckill -am test -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

Run:

```bash
git add mall-backend/pom.xml mall-backend/mall-app/pom.xml mall-backend/mall-seckill
git commit -m "feat: add seckill module skeleton"
```

---

### Task 3: Add Seckill Entities, Mappers, DTOs, And VOs

**Files:**
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/entity/SeckillActivity.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/entity/SeckillSku.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/mapper/SeckillActivityMapper.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/mapper/SeckillSkuMapper.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/dto/SeckillOrderCreateRequest.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/vo/SeckillActivityVO.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/vo/SeckillSkuVO.java`

- [ ] **Step 1: Add a failing compile check**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-seckill -am test -DskipTests
```

Expected before files exist: current module compiles, but later controller/service references will fail until these types are added.

- [ ] **Step 2: Add entity classes with Chinese JavaDoc**

Create `SeckillActivity.java` with fields matching `sms_seckill_activity`: `id`, `name`, `startTime`, `endTime`, `status`, `remark`, `createTime`, `updateTime`, `deleted`.

Create `SeckillSku.java` with fields matching `sms_seckill_sku`: `id`, `activityId`, `skuId`, `seckillPrice`, `stockCount`, `limitQuantity`, `sort`, `status`, `createTime`, `updateTime`, `deleted`.

Both classes should use:

```java
/**
 * 秒杀活动实体，映射秒杀活动主表并描述活动时间窗口和启用状态。
 */
```

```java
/**
 * 秒杀活动商品实体，映射某场秒杀活动中的 SKU、秒杀价、活动库存和限购规则。
 */
```

- [ ] **Step 3: Add mapper interfaces**

Create `SeckillActivityMapper.java`:

```java
/**
 * 秒杀活动 Mapper，负责秒杀活动主表的基础数据访问。
 */
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {
}
```

Create `SeckillSkuMapper.java`:

```java
/**
 * 秒杀活动商品 Mapper，负责秒杀活动商品表的基础数据访问。
 */
public interface SeckillSkuMapper extends BaseMapper<SeckillSku> {
}
```

- [ ] **Step 4: Add request and response objects**

`SeckillOrderCreateRequest` should include validation:

```java
@NotNull(message = "seckillSkuId must not be null")
private Long seckillSkuId;

@NotBlank(message = "requestId must not be blank")
private String requestId;

@NotNull(message = "addressId must not be null")
private Long addressId;

@NotNull(message = "quantity must not be null")
@Min(value = 1, message = "quantity must be greater than 0")
private Integer quantity;

private String remark;
```

`SeckillActivityVO` should include: `id`, `name`, `startTime`, `endTime`, `status`, `List<SeckillSkuVO> skus`.

`SeckillSkuVO` should include: `id`, `activityId`, `skuId`, `productName`, `skuName`, `mainImageUrl`, `seckillPrice`, `stockCount`, `limitQuantity`, `status`.

- [ ] **Step 5: Verify compile**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-seckill -am test -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

Run:

```bash
git add mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill
git commit -m "feat: add seckill data model"
```

---

### Task 4: Extend Order Service With Price Overrides

**Files:**
- Modify: `mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/OrderService.java`
- Modify: `mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/impl/OrderServiceImpl.java`
- Test: `mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java` later covers end-to-end price override.

- [ ] **Step 1: Add a failing service contract compile check**

Add a temporary test reference in the future seckill service plan step or run compile after adding the interface method and before implementation.

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-order -am test -DskipTests
```

Expected after only interface change: FAIL until `OrderServiceImpl` implements the new method.

- [ ] **Step 2: Add nested price override contract to `OrderService`**

Add:

```java
/**
 * 使用调用方提供的 SKU 价格快照创建订单，适用于秒杀等特殊价格场景。
 *
 * @param userId 当前登录用户 ID，由登录态解析得到
 * @param request 创建订单请求，包含收货地址 ID、幂等请求号、订单明细列表和备注
 * @param priceOverrides SKU ID 到下单单价的映射，传入的 SKU 必须覆盖本次订单中的每个 SKU
 * @return 创建后的订单核心信息
 */
OrderCreateVO createOrderWithPriceOverrides(Long userId, OrderCreateRequest request, Map<Long, BigDecimal> priceOverrides);
```

Import `java.math.BigDecimal` and `java.util.Map`.

- [ ] **Step 3: Refactor implementation through one private method**

In `OrderServiceImpl`, keep existing method:

```java
@Override
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public OrderCreateVO createOrder(Long userId, OrderCreateRequest request) {
    return createOrderInternal(userId, request, Map.of());
}
```

Add:

```java
@Override
@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
public OrderCreateVO createOrderWithPriceOverrides(Long userId,
                                                   OrderCreateRequest request,
                                                   Map<Long, BigDecimal> priceOverrides) {
    if (priceOverrides == null || priceOverrides.isEmpty()) {
        throw new BusinessException(400, "price overrides must not be empty");
    }
    return createOrderInternal(userId, request, priceOverrides);
}
```

Move the current body of `createOrder` into:

```java
private OrderCreateVO createOrderInternal(Long userId,
                                          OrderCreateRequest request,
                                          Map<Long, BigDecimal> priceOverrides) {
    // existing createOrder body
}
```

- [ ] **Step 4: Use override price during amount and item snapshot calculation**

Change amount calculation to use:

```java
private BigDecimal resolveUnitPrice(Sku sku, Map<Long, BigDecimal> priceOverrides) {
    BigDecimal overridePrice = priceOverrides.get(sku.getId());
    if (overridePrice == null) {
        return sku.getPrice();
    }
    if (overridePrice.compareTo(BigDecimal.ZERO) <= 0) {
        throw new BusinessException(400, "override price must be greater than 0");
    }
    return overridePrice;
}
```

Then use `resolveUnitPrice(...)` in `calculateTotalAmount(...)` and `buildOrderItem(...)`.

- [ ] **Step 5: Verify normal order tests still pass**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-app -Dtest=OrderCreateApiIntegrationTest test
```

Expected: BUILD SUCCESS and existing order API behavior unchanged.

- [ ] **Step 6: Commit**

Run:

```bash
git add mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/OrderService.java mall-backend/mall-order/src/main/java/com/tuzki/mall/order/service/impl/OrderServiceImpl.java
git commit -m "feat: support order price overrides"
```

---

### Task 5: Implement Seckill Redis Service

**Files:**
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/redis/SeckillRedisService.java`

- [ ] **Step 1: Add failing compile reference**

`SeckillServiceImpl` in Task 6 will inject this service. Before creating it, that compile should fail with `cannot find symbol SeckillRedisService`.

- [ ] **Step 2: Implement Redis service**

Create a Spring service with:

```java
/**
 * 秒杀 Redis 服务，负责活动库存预热、Lua 原子预扣和失败补偿。
 */
@Service
public class SeckillRedisService {
    public static final long PRE_DEDUCT_SUCCESS = 0L;
    public static final long DUPLICATED_REQUEST = 1L;
    public static final long PURCHASE_LIMIT_EXCEEDED = 2L;
    public static final long STOCK_SOLD_OUT = 3L;
    public static final long STOCK_NOT_PREHEATED = 4L;

    public void preheatStock(Long seckillSkuId, Integer stockCount, Duration ttl) { ... }
    public long preDeduct(Long seckillSkuId, Long userId, String requestId, Integer quantity, Integer limitQuantity, Duration ttl) { ... }
    public void compensate(Long seckillSkuId, Long userId, String requestId, Integer quantity) { ... }
}
```

Use keys:

```java
private String stockKey(Long seckillSkuId) {
    return "mall:seckill:stock:" + seckillSkuId;
}

private String userKey(Long seckillSkuId, Long userId) {
    return "mall:seckill:user:" + seckillSkuId + ":" + userId;
}

private String requestKey(Long seckillSkuId, Long userId, String requestId) {
    return "mall:seckill:request:" + seckillSkuId + ":" + userId + ":" + requestId;
}
```

Lua script logic:

```lua
local stock = redis.call('GET', KEYS[1])
if not stock then return 4 end
if redis.call('EXISTS', KEYS[3]) == 1 then return 1 end
local current = redis.call('GET', KEYS[2])
if current and tonumber(current) + tonumber(ARGV[1]) > tonumber(ARGV[2]) then return 2 end
if tonumber(stock) < tonumber(ARGV[1]) then return 3 end
redis.call('DECRBY', KEYS[1], ARGV[1])
redis.call('INCRBY', KEYS[2], ARGV[1])
redis.call('SET', KEYS[3], '1')
redis.call('EXPIRE', KEYS[1], ARGV[3])
redis.call('EXPIRE', KEYS[2], ARGV[3])
redis.call('EXPIRE', KEYS[3], ARGV[3])
return 0
```

Use `redissonClient.getScript(StringCodec.INSTANCE).eval(...)`.

- [ ] **Step 3: Implement compensation carefully**

Compensation should:

```text
INCRBY stock key quantity
DECRBY user key quantity
delete user key if value <= 0
delete request key
```

Use a small Lua script so the cleanup is atomic.

- [ ] **Step 4: Verify compile**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-seckill -am test -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

Run:

```bash
git add mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/redis/SeckillRedisService.java
git commit -m "feat: add seckill redis stock service"
```

---

### Task 6: Implement Seckill Service And Controllers

**Files:**
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/SeckillService.java`
- Create: `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/service/impl/SeckillServiceImpl.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/controller/SeckillController.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/controller/SeckillAdminController.java`

- [ ] **Step 1: Add failing API test skeleton**

Create `SeckillApiIntegrationTest` with one test calling:

```java
mockMvc.perform(get("/api/seckill/activities/active"))
        .andExpect(status().isOk());
```

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-app -Dtest=SeckillApiIntegrationTest test
```

Expected before controllers exist: FAIL with 404 or missing route.

- [ ] **Step 2: Add service interface**

```java
/**
 * 秒杀业务接口，负责查询当前活动、预热活动库存以及创建秒杀订单。
 */
public interface SeckillService {
    /**
     * 查询当前时间可参与的秒杀活动。
     *
     * @return 当前有效秒杀活动列表
     */
    List<SeckillActivityVO> listActiveActivities();

    /**
     * 预热指定活动下的秒杀商品库存到 Redis。
     *
     * @param activityId 秒杀活动 ID
     */
    void preheatActivity(Long activityId);

    /**
     * 创建秒杀订单。
     *
     * @param userId 当前登录用户 ID
     * @param request 秒杀下单请求
     * @return 创建后的订单核心信息
     */
    OrderCreateVO createSeckillOrder(Long userId, SeckillOrderCreateRequest request);
}
```

- [ ] **Step 3: Implement service orchestration**

`SeckillServiceImpl` should:

1. Query active activities with `status = 1`, `deleted = 0`, `startTime <= now`, `endTime >= now`.
2. Query enabled activity SKUs and join product/SKU snapshots through `ProductMapper` and `SkuMapper`.
3. Preheat all enabled SKUs for an activity with TTL `Duration.between(now, endTime).plusMinutes(5)`.
4. In `createSeckillOrder`, validate activity and SKU status, time window, quantity and limit.
5. Call `SeckillRedisService.preDeduct(...)`.
6. For duplicate request, continue to order idempotent query by calling order service with the same prefixed request ID.
7. For other Redis failure codes, throw `BusinessException` with the messages from the spec.
8. Build one-item `OrderCreateRequest`, set `requestId = "seckill:" + seckillSkuId + ":" + request.getRequestId()`.
9. Call `orderService.createOrderWithPriceOverrides(userId, orderRequest, Map.of(skuId, seckillPrice))`.
10. On runtime failure after pre-deduction, call `seckillRedisService.compensate(...)` and rethrow.

- [ ] **Step 4: Add controllers**

`SeckillController`:

```java
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    @GetMapping("/activities/active")
    public Result<List<SeckillActivityVO>> listActiveActivities() { ... }

    @PostMapping("/orders")
    public Result<OrderCreateVO> createSeckillOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SeckillOrderCreateRequest request) { ... }
}
```

`SeckillAdminController`:

```java
@RestController
@RequestMapping("/api/admin/seckill")
public class SeckillAdminController {
    @PostMapping("/activities/{activityId}/preheat")
    public Result<Void> preheatActivity(@PathVariable Long activityId) { ... }
}
```

Copy the login-token parsing style from `OrderController` for MVP.

- [ ] **Step 5: Verify API route test passes**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-app -Dtest=SeckillApiIntegrationTest test
```

Expected: initial route test passes.

- [ ] **Step 6: Commit**

Run:

```bash
git add mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java
git commit -m "feat: add seckill service APIs"
```

---

### Task 7: Add End-To-End Seckill Tests

**Files:**
- Modify: `mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java`

- [ ] **Step 1: Add test data helpers**

Inside `SeckillApiIntegrationTest`, insert activity and seckill SKU records through `SeckillActivityMapper` and `SeckillSkuMapper` in helper methods:

```java
private SeckillSku createActiveSeckillSku(BigDecimal price, int stockCount, int limitQuantity) {
    SeckillActivity activity = new SeckillActivity();
    activity.setName("test seckill");
    activity.setStartTime(LocalDateTime.now().minusMinutes(5));
    activity.setEndTime(LocalDateTime.now().plusMinutes(30));
    activity.setStatus(1);
    activity.setDeleted(0);
    seckillActivityMapper.insert(activity);

    SeckillSku seckillSku = new SeckillSku();
    seckillSku.setActivityId(activity.getId());
    seckillSku.setSkuId(TestSeedData.SKU_ID);
    seckillSku.setSeckillPrice(price);
    seckillSku.setStockCount(stockCount);
    seckillSku.setLimitQuantity(limitQuantity);
    seckillSku.setSort(0);
    seckillSku.setStatus(1);
    seckillSku.setDeleted(0);
    seckillSkuMapper.insert(seckillSku);
    return seckillSku;
}
```

- [ ] **Step 2: Test active activity listing**

Add:

```java
@Test
void listActiveActivitiesReturnsEnabledCurrentActivity() throws Exception {
    SeckillSku seckillSku = createActiveSeckillSku(new BigDecimal("39.90"), 5, 1);

    mockMvc.perform(get("/api/seckill/activities/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].skus[0].id").value(seckillSku.getId()))
            .andExpect(jsonPath("$.data[0].skus[0].seckillPrice").value(39.90));
}
```

- [ ] **Step 3: Test preheat and successful seckill order**

Add a test that:

1. Resets real inventory to 100.
2. Creates seckill SKU stock 2 and price `39.90`.
3. Calls admin preheat.
4. Calls `POST /api/seckill/orders`.
5. Verifies order total and item unit price are `39.90`.
6. Verifies real inventory locked stock increased by 1.

- [ ] **Step 4: Test stock not preheated**

Call `POST /api/seckill/orders` before preheat.

Expected:

```text
$.success = false
$.code = 400
$.message = seckill stock not preheated
```

- [ ] **Step 5: Test sold out**

Preheat stock `1`, submit two different users or requests for quantity `1`. The second should return:

```text
$.message = seckill stock sold out
```

- [ ] **Step 6: Test purchase limit**

Create stock `5`, limit `1`, preheat, submit first request successfully, then second request with a different `requestId`.

Expected second response:

```text
$.message = seckill purchase limit exceeded
```

- [ ] **Step 7: Test duplicate request idempotency**

Submit the same request twice.

Expected:

1. Same `orderId` returned.
2. Only one order exists for request ID `seckill:{seckillSkuId}:{requestId}`.
3. Redis stock only decremented once.

- [ ] **Step 8: Test compensation on order failure**

Set real MySQL inventory to `0`, preheat Redis stock to `1`, submit seckill order.

Expected:

```text
$.message = insufficient stock
```

Then submit after resetting real inventory to `1` with a new request ID and verify Redis stock was compensated enough to allow success.

- [ ] **Step 9: Verify tests pass**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-app -Dtest=SeckillApiIntegrationTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 10: Commit**

Run:

```bash
git add mall-backend/mall-app/src/test/java/com/tuzki/mall/seckill/SeckillApiIntegrationTest.java
git commit -m "test: cover seckill order flow"
```

---

### Task 8: Full Verification

**Files:**
- No new files unless verification exposes a bug.

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
mvn -f mall-backend/pom.xml -pl mall-app -Dtest=SeckillApiIntegrationTest,OrderCreateApiIntegrationTest test
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run full backend test suite**

Run:

```bash
mvn -f mall-backend/pom.xml test
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Check git status**

Run:

```bash
git status --short
```

Expected: only unrelated pre-existing `.idea/` files are untracked, or no changes after final commit.

- [ ] **Step 4: Commit final fixes if needed**

If verification required fixes:

```bash
git add <fixed-files>
git commit -m "fix: stabilize seckill mvp"
```

Expected: commit succeeds.

---

## Self-Review

Spec coverage:

1. Activity and activity SKU schema: Task 1.
2. Independent `mall-seckill` module: Task 2.
3. Entities, mappers, DTOs, VOs: Task 3.
4. Seckill price order creation: Task 4 and Task 7.
5. Redis stock preheat, Lua pre-deduction, request idempotency, user limit, compensation: Task 5 and Task 7.
6. Public and admin APIs: Task 6.
7. Activity status, stock, limit, idempotency, compensation, price, and concurrency-oriented stock cap tests: Task 7.
8. Final Maven verification: Task 8.

No placeholders are intentionally left. The plan keeps the MVP synchronous and does not include MQ queueing, request流水, automatic preheat scheduling, frontend pages, anti-bot protection, or hidden URLs.
