# Shopping Cart Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a logged-in shopping cart backed by Redis, asynchronously persisted to MySQL through RabbitMQ, with multi-item checkout in the Vue frontend.

**Architecture:** Add a focused `mall-cart` Maven module containing the cart domain, Redis cache service, version-aware persistence service, and MQ contract. Keep HTTP controllers and RabbitMQ adapters in `mall-app`. Redis stores live cart-item JSON values including delete tombstones; RabbitMQ carries item-level change events; MySQL applies only events with a greater version and rebuilds Redis after cache loss.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis-Plus, Redisson, RabbitMQ, MySQL 8, JUnit 5, MockMvc, Vue 3, TypeScript, Element Plus, Vite

---

## File Map

### Backend module and schema

- Create `scripts/mysql/014_cart_schema.sql`: define `oms_cart_item`.
- Modify `mall-backend/pom.xml`: register `mall-cart`.
- Create `mall-backend/mall-cart/pom.xml`: depend on `mall-common`, `mall-product`, and Redisson.
- Modify `mall-backend/mall-app/pom.xml`: aggregate `mall-cart`.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/package-info.java`: document module boundary.

### Backend cart domain

- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/entity/CartItem.java`: MySQL entity.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartAddRequest.java`: add request.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartQuantityUpdateRequest.java`: quantity replacement request.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartBatchDeleteRequest.java`: checkout cleanup request.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/vo/CartItemVO.java`: live product display model.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeOperation.java`: `UPSERT` and `DELETE`.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeMessage.java`: versioned event.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeMessageSender.java`: producer boundary.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/mapper/CartItemMapper.java`: version-aware MySQL writes.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartCacheService.java`: Redis Lua scripts, tombstones, and rebuild.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartService.java`: cart use-case interface.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/impl/CartServiceImpl.java`: cart orchestration.
- Create `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartPersistenceService.java`: consume versioned changes into MySQL.

### Backend app adapters

- Create `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/CartRabbitProperties.java`: exchange, queue, route, confirm timeout.
- Create `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/CartRabbitConfig.java`: durable direct exchange and queue.
- Create `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/mq/CartChangeProducer.java`: publish with Publisher Confirm.
- Create `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/mq/CartChangeConsumer.java`: call persistence service.
- Create `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/controller/CartController.java`: authenticated REST API.
- Modify `mall-backend/mall-app/src/main/resources/application.yml`: enable confirms and cart RabbitMQ names.

### Backend tests

- Create `mall-backend/mall-app/src/test/java/com/tuzki/mall/config/rabbit/CartRabbitConfigTest.java`.
- Create `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/mq/CartChangeProducerTest.java`.
- Create `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartPersistenceServiceIntegrationTest.java`.
- Create `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartApiIntegrationTest.java`.

### Frontend

- Create `mall-frontend/src/api/cart.ts`: typed cart calls.
- Create `mall-frontend/src/utils/checkoutStorage.ts`: session-storage checkout payload.
- Create `mall-frontend/src/views/CartView.vue`: selectable cart.
- Modify `mall-frontend/src/views/ProductDetailView.vue`: wire add-to-cart.
- Modify `mall-frontend/src/views/OrderConfirmView.vue`: support buy-now and cart checkout.
- Modify `mall-frontend/src/components/SiteHeader.vue`: route cart entry to `/cart`.
- Modify `mall-frontend/src/router/index.ts`: register authenticated `/cart`.

## Task 1: Add Schema And Maven Module

**Files:**
- Create: `scripts/mysql/014_cart_schema.sql`
- Modify: `mall-backend/pom.xml`
- Create: `mall-backend/mall-cart/pom.xml`
- Modify: `mall-backend/mall-app/pom.xml`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/package-info.java`

- [ ] **Step 1: Add the cart schema**

```sql
USE mall_lite;

CREATE TABLE IF NOT EXISTS oms_cart_item
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    sku_id      BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
    quantity    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '数量，逻辑删除墓碑为0',
    version     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '购物车项版本号',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku (user_id, sku_id),
    KEY idx_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '购物车项表';
```

- [ ] **Step 2: Register the new Maven module**

Add `<module>mall-cart</module>` before `mall-payment` in `mall-backend/pom.xml`. Create `mall-cart/pom.xml` using the existing module style with dependencies on `mall-common`, `mall-product`, and `org.redisson:redisson-spring-boot-starter`. Add `com.tuzki:mall-cart:${project.version}` to `mall-app/pom.xml`.

- [ ] **Step 3: Add module documentation**

```java
/**
 * 购物车模块，负责登录用户购物车的实时缓存、异步持久化和查询展示。
 */
package com.tuzki.mall.cart;
```

- [ ] **Step 4: Verify Maven recognizes the module**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-cart -am test -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```powershell
git add scripts/mysql/014_cart_schema.sql mall-backend/pom.xml mall-backend/mall-cart mall-backend/mall-app/pom.xml
git commit -m "feat: add cart module and schema"
```

## Task 2: Add Version-Aware MySQL Persistence

**Files:**
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/entity/CartItem.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeOperation.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeMessage.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/mapper/CartItemMapper.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartPersistenceService.java`
- Create: `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartPersistenceServiceIntegrationTest.java`

- [ ] **Step 1: Write failing persistence integration tests**

Create tests that call `CartPersistenceService.apply(...)` with:

```java
new CartChangeMessage(USER_ID, SKU_ID, 2, 1L, CartChangeOperation.UPSERT)
new CartChangeMessage(USER_ID, SKU_ID, 5, 3L, CartChangeOperation.UPSERT)
new CartChangeMessage(USER_ID, SKU_ID, 3, 2L, CartChangeOperation.UPSERT)
new CartChangeMessage(USER_ID, SKU_ID, 0, 4L, CartChangeOperation.DELETE)
```

Assert after each step:

```java
assertThat(item.getQuantity()).isEqualTo(5);
assertThat(item.getVersion()).isEqualTo(3L);
assertThat(item.getDeleted()).isZero();
```

and after delete:

```java
assertThat(item.getQuantity()).isZero();
assertThat(item.getVersion()).isEqualTo(4L);
assertThat(item.getDeleted()).isEqualTo(1);
```

Reapply version `3` and assert the delete tombstone remains unchanged.

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartPersistenceServiceIntegrationTest test
```

Expected: FAIL because cart persistence classes do not exist.

- [ ] **Step 3: Implement entity, message, mapper, and service**

Use a Java record for the immutable event:

```java
/**
 * 购物车逐项变更消息，携带用户、SKU、数量、版本号和操作类型。
 *
 * @param userId 用户 ID
 * @param skuId SKU ID
 * @param quantity 最新数量，删除消息固定为 0
 * @param version 购物车项单调递增版本号
 * @param operation 变更操作
 */
public record CartChangeMessage(Long userId,
                                Long skuId,
                                Integer quantity,
                                Long version,
                                CartChangeOperation operation) {
}
```

Implement one version-aware MySQL upsert:

```java
@Insert("""
        INSERT INTO oms_cart_item(user_id, sku_id, quantity, version, deleted)
        VALUES(#{userId}, #{skuId}, #{quantity}, #{version}, #{deleted})
        ON DUPLICATE KEY UPDATE
            quantity = IF(VALUES(version) > version, VALUES(quantity), quantity),
            deleted = IF(VALUES(version) > version, VALUES(deleted), deleted),
            update_time = IF(VALUES(version) > version, NOW(), update_time),
            version = GREATEST(version, VALUES(version))
        """)
int upsertIfNewer(@Param("userId") Long userId,
                  @Param("skuId") Long skuId,
                  @Param("quantity") Integer quantity,
                  @Param("version") Long version,
                  @Param("deleted") Integer deleted);
```

`CartPersistenceService.apply(...)` maps `UPSERT` to `deleted = 0` and `DELETE` to `quantity = 0, deleted = 1`.

- [ ] **Step 4: Run persistence tests**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartPersistenceServiceIntegrationTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add mall-backend/mall-cart mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartPersistenceServiceIntegrationTest.java
git commit -m "feat: persist versioned cart changes"
```

## Task 3: Add Redis Tombstones And Conditional Rollback

**Files:**
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartCacheService.java`
- Create: `mall-backend/mall-cart/src/test/java/com/tuzki/mall/cart/service/CartCacheServiceTest.java`

- [ ] **Step 1: Write failing Redis cache tests**

Cover:

1. Adding a new SKU returns `{quantity: 2, version: 1, deleted: false}`.
2. Adding the same SKU again returns quantity `5`, version `2`.
3. Deleting writes `{quantity: 0, version: 3, deleted: true}` instead of removing the Hash field.
4. Adding after delete returns quantity `1`, version `4`.
5. Rollback with the current version restores the previous JSON.
6. Rollback with a stale version does nothing.
7. Rebuild loads active values and tombstones, then sets `mall:cart:loaded:{userId}`.

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-cart -am -Dtest=CartCacheServiceTest test
```

Expected: FAIL because `CartCacheService` does not exist.

- [ ] **Step 3: Implement Redis JSON value and Lua scripts**

Use:

```java
/**
 * Redis 中保存的购物车项值，删除时保留墓碑和版本号。
 *
 * @param quantity 当前数量，墓碑固定为 0
 * @param version 单调递增版本号
 * @param deleted 是否为删除墓碑
 */
public record CartCacheItem(Integer quantity, Long version, boolean deleted) {
}
```

Use `RScript.eval(...)` with `StringCodec.INSTANCE`. The mutation Lua script must:

```lua
local old = redis.call('HGET', KEYS[1], ARGV[1])
local oldValue = old and cjson.decode(old) or { quantity = 0, version = 0, deleted = true }
local quantity = tonumber(ARGV[2])
local deleted = ARGV[3] == 'true'
local newValue = cjson.encode({
  quantity = quantity,
  version = tonumber(oldValue.version) + 1,
  deleted = deleted
})
redis.call('HSET', KEYS[1], ARGV[1], newValue)
redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])
return { old or '', newValue }
```

The rollback script must compare the current JSON `version` with the failed request version before restoring `oldValue` or deleting a newly created field.

- [ ] **Step 4: Run Redis cache tests**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-cart -am -Dtest=CartCacheServiceTest test
```

Expected: PASS while local Redis is running.

- [ ] **Step 5: Commit**

```powershell
git add mall-backend/mall-cart
git commit -m "feat: add redis cart tombstones"
```

## Task 4: Add Cart Service And REST API

**Files:**
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartAddRequest.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartQuantityUpdateRequest.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/dto/CartBatchDeleteRequest.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/vo/CartItemVO.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/message/CartChangeMessageSender.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartService.java`
- Create: `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/impl/CartServiceImpl.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/controller/CartController.java`
- Create: `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartApiIntegrationTest.java`

- [ ] **Step 1: Write failing MockMvc integration tests**

Cover:

```text
POST /api/cart/items                    add
POST /api/cart/items                    repeated add accumulates
PUT /api/cart/items/{skuId}             replace quantity
GET /api/cart/items                     list latest product data
DELETE /api/cart/items/{skuId}          write tombstone
DELETE /api/cart/items                  batch delete
```

Also assert missing token, invalid token, quantity `0`, and accumulated quantity `100` are rejected.

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartApiIntegrationTest test
```

Expected: FAIL with missing cart API.

- [ ] **Step 3: Add validated DTOs and the service interface**

Example add request:

```java
/**
 * 加入购物车请求。
 */
public class CartAddRequest {
    @NotNull(message = "skuId must not be null")
    private Long skuId;

    @NotNull(message = "quantity must not be null")
    @Min(value = 1, message = "quantity must be greater than 0")
    @Max(value = 99, message = "quantity must not be greater than 99")
    private Integer quantity;
}
```

`CartService` methods:

```java
void add(Long userId, CartAddRequest request);
List<CartItemVO> list(Long userId);
void updateQuantity(Long userId, Long skuId, CartQuantityUpdateRequest request);
void delete(Long userId, Long skuId);
void batchDelete(Long userId, CartBatchDeleteRequest request);
```

- [ ] **Step 4: Implement orchestration**

`CartServiceImpl` must:

1. Ensure Redis is loaded before every mutation so version increments continue from MySQL tombstones after Redis loss.
2. Validate active SKU and active product before add.
3. Reject accumulated quantities greater than `99`.
4. Call `CartCacheService.mutate(...)`.
5. Send one `CartChangeMessage`.
6. On send failure call conditional rollback with the written version, then throw `BusinessException(503, "cart change message send failed")`.
7. Rebuild from all MySQL cart rows, including tombstones.
8. List only non-deleted cache values and enrich them with current SKU and product data.
9. Set `available = sku.status == 1 && product.status == 1 && sku.deleted == 0 && product.deleted == 0`.

- [ ] **Step 5: Add authenticated controller**

Use the existing bearer-token parsing style from `OrderController`. Keep every endpoint under:

```java
@RequestMapping("/api/cart/items")
```

- [ ] **Step 6: Run API tests**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartApiIntegrationTest test
```

Expected: PASS with the sender stubbed for API tests.

- [ ] **Step 7: Commit**

```powershell
git add mall-backend/mall-cart mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/controller mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/CartApiIntegrationTest.java
git commit -m "feat: add shopping cart api"
```

## Task 5: Add RabbitMQ Adapters And Publisher Confirms

**Files:**
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/CartRabbitProperties.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/config/rabbit/CartRabbitConfig.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/mq/CartChangeProducer.java`
- Create: `mall-backend/mall-app/src/main/java/com/tuzki/mall/cart/mq/CartChangeConsumer.java`
- Modify: `mall-backend/mall-app/src/main/resources/application.yml`
- Create: `mall-backend/mall-app/src/test/java/com/tuzki/mall/config/rabbit/CartRabbitConfigTest.java`
- Create: `mall-backend/mall-app/src/test/java/com/tuzki/mall/cart/mq/CartChangeProducerTest.java`

- [ ] **Step 1: Write failing RabbitMQ configuration and producer tests**

Assert configured names:

```text
mall.cart.change.exchange
mall.cart.change.queue
mall.cart.change.routing-key
```

Mock `RabbitTemplate.invoke(...)` and verify the producer calls:

```java
operations.convertAndSend(exchange, routingKey, message);
operations.waitForConfirms(Duration.ofSeconds(3));
```

Assert a negative confirm throws `BusinessException(503, "cart change message send failed")`.

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartRabbitConfigTest,CartChangeProducerTest test
```

Expected: FAIL because RabbitMQ cart adapters do not exist.

- [ ] **Step 3: Implement RabbitMQ adapters**

Enable Publisher Confirm:

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: simple
```

Add cart config:

```yaml
mall:
  cart:
    rabbit:
      change-exchange: mall.cart.change.exchange
      change-queue: mall.cart.change.queue
      change-routing-key: mall.cart.change.routing-key
      confirm-timeout-seconds: 3
```

Producer core:

```java
boolean confirmed = rabbitTemplate.invoke(operations -> {
    operations.convertAndSend(properties.getChangeExchange(),
            properties.getChangeRoutingKey(), message);
    return operations.waitForConfirms(Duration.ofSeconds(properties.getConfirmTimeoutSeconds()));
});
if (!confirmed) {
    throw new BusinessException(503, "cart change message send failed");
}
```

Consumer:

```java
@RabbitListener(queues = "${mall.cart.rabbit.change-queue}")
public void handle(CartChangeMessage message) {
    cartPersistenceService.apply(message);
}
```

- [ ] **Step 4: Run RabbitMQ adapter tests**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartRabbitConfigTest,CartChangeProducerTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add mall-backend/mall-app
git commit -m "feat: sync cart changes through rabbitmq"
```

## Task 6: Add Frontend Cart API And Page

**Files:**
- Create: `mall-frontend/src/api/cart.ts`
- Create: `mall-frontend/src/utils/checkoutStorage.ts`
- Create: `mall-frontend/src/views/CartView.vue`
- Modify: `mall-frontend/src/router/index.ts`
- Modify: `mall-frontend/src/components/SiteHeader.vue`

- [ ] **Step 1: Add typed cart API**

```ts
export interface CartItem {
  skuId: number;
  productId: number;
  productName: string;
  skuName: string;
  mainImageUrl: string;
  price: number;
  quantity: number;
  available: boolean;
}

export function listCartItems() {
  return http.get<Result<CartItem[]>>('/api/cart/items');
}

export function addCartItem(request: { skuId: number; quantity: number }) {
  return http.post<Result<void>>('/api/cart/items', request);
}

export function updateCartItemQuantity(skuId: number, quantity: number) {
  return http.put<Result<void>>(`/api/cart/items/${skuId}`, { quantity });
}

export function deleteCartItem(skuId: number) {
  return http.delete<Result<void>>(`/api/cart/items/${skuId}`);
}

export function deleteCartItems(skuIds: number[]) {
  return http.delete<Result<void>>('/api/cart/items', { data: { skuIds } });
}
```

- [ ] **Step 2: Add checkout session storage**

Store selected items in `sessionStorage` instead of a long query string:

```ts
const CART_CHECKOUT_KEY = 'mall-lite-cart-checkout';

export interface CheckoutItem {
  productId: number;
  skuId: number;
  quantity: number;
}
```

Expose `setCartCheckoutItems`, `getCartCheckoutItems`, and `clearCartCheckoutItems`.

- [ ] **Step 3: Add `/cart` route and correct header link**

Register authenticated route:

```ts
{
  path: '/cart',
  name: 'cart',
  component: () => import('@/views/CartView.vue'),
  meta: { requiresAuth: true }
}
```

Change `SiteHeader.vue` cart target from `/` to `/cart`.

- [ ] **Step 4: Build the cart page**

`CartView.vue` must provide:

1. Loading and empty states.
2. Checkbox per available item.
3. Disabled checkbox and “商品已失效” tag for unavailable items.
4. Quantity stepper with `min=1`, `max=99`, persisting changes.
5. Delete action with confirmation.
6. Select-all checkbox that only selects available items.
7. Sticky summary showing selected quantity and total.
8. Checkout button that stores selected `productId + skuId + quantity` and routes to `/order/confirm?source=cart`.

- [ ] **Step 5: Run frontend build**

Run:

```powershell
cd mall-frontend
npm run build
```

Expected: `vue-tsc --noEmit` and `vite build` succeed.

- [ ] **Step 6: Commit**

```powershell
git add mall-frontend/src/api/cart.ts mall-frontend/src/utils/checkoutStorage.ts mall-frontend/src/views/CartView.vue mall-frontend/src/router/index.ts mall-frontend/src/components/SiteHeader.vue
git commit -m "feat: add shopping cart page"
```

## Task 7: Wire Product Add-To-Cart And Multi-Item Checkout

**Files:**
- Modify: `mall-frontend/src/views/ProductDetailView.vue`
- Modify: `mall-frontend/src/views/OrderConfirmView.vue`

- [ ] **Step 1: Wire product-detail add-to-cart**

Import `addCartItem`, add `cartSubmitting`, and implement:

```ts
async function addToCart() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格');
    return;
  }
  if (!authStore.isLoggedIn) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } });
    return;
  }
  cartSubmitting.value = true;
  try {
    await addCartItem({ skuId: selectedSku.value.id, quantity: quantity.value });
    ElMessage.success('已加入购物车');
  } catch {
    ElMessage.error('加入购物车失败');
  } finally {
    cartSubmitting.value = false;
  }
}
```

Bind this handler and loading state to the existing cart button.

- [ ] **Step 2: Refactor order-confirm input normalization**

Replace the single computed item with:

```ts
const source = computed(() => route.query.source === 'cart' ? 'cart' : 'buy-now');
const checkoutItems = ref<CheckoutItem[]>([]);
const products = ref<Record<number, ProductDetail>>({});
const selectedSkus = computed(() => checkoutItems.value.map((item) => {
  const product = products.value[item.productId];
  const sku = product?.skus.find((candidate) => candidate.id === item.skuId);
  return { item, product, sku };
}));
```

For `source=cart`, read `sessionStorage`. For buy-now, normalize the existing query parameters into a one-item array.

- [ ] **Step 3: Submit multi-item orders and clean cart after success**

Build order items with:

```ts
items: selectedSkus.value.map(({ item }) => ({
  skuId: item.skuId,
  quantity: item.quantity
}))
```

After successful order creation:

```ts
if (source.value === 'cart') {
  await deleteCartItems(checkoutItems.value.map((item) => item.skuId))
    .catch(() => ElMessage.warning('订单已创建，但购物车清理失败，请稍后手动处理'));
  clearCartCheckoutItems();
}
await router.replace(`/order/result/${response.data.data.orderId}`);
```

- [ ] **Step 4: Render multiple goods rows**

Change the existing single `.goods-row` into `v-for="entry in selectedSkus"` and calculate:

```ts
const goodsAmount = computed(() => selectedSkus.value.reduce(
  (sum, { item, sku }) => sum + Number((sku?.price || 0) * item.quantity),
  0
));
```

- [ ] **Step 5: Run frontend build**

Run:

```powershell
cd mall-frontend
npm run build
```

Expected: build succeeds.

- [ ] **Step 6: Commit**

```powershell
git add mall-frontend/src/views/ProductDetailView.vue mall-frontend/src/views/OrderConfirmView.vue
git commit -m "feat: checkout selected cart items"
```

## Task 8: Apply Schema And Verify End-To-End

**Files:**
- Verify: all cart files
- Preserve: existing user changes in `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/impl/ProductFootprintServiceImpl.java`

- [ ] **Step 1: Apply cart schema**

Run:

```powershell
Get-Content scripts/mysql/014_cart_schema.sql -Raw | mysql -uroot -p mall_lite
```

Expected: table `oms_cart_item` exists. Enter the configured local MySQL password when prompted.

- [ ] **Step 2: Run backend cart tests**

Run:

```powershell
mvn -f mall-backend/pom.xml -pl mall-app -am -Dtest=CartPersistenceServiceIntegrationTest,CartCacheServiceTest,CartRabbitConfigTest,CartChangeProducerTest,CartApiIntegrationTest test
```

Expected: PASS.

- [ ] **Step 3: Run backend regression tests**

Run:

```powershell
mvn -f mall-backend/pom.xml test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run frontend build**

Run:

```powershell
cd mall-frontend
npm run build
```

Expected: build succeeds.

- [ ] **Step 5: Start app and verify with the in-app browser**

Start the Spring Boot backend and Vite dev server. Verify:

1. Login.
2. Add a SKU from product detail.
3. Open `/cart`.
4. Change quantity and select items.
5. Continue to order confirmation.
6. Create the order.
7. Return to `/cart` and confirm only checked items were removed.
8. Confirm “立即购买” still creates an order without removing unrelated cart items.

- [ ] **Step 6: Inspect final worktree**

Run:

```powershell
git status --short
git diff --check
```

Expected: no whitespace errors; do not stage or modify the existing footprint-service user change or `.idea` files.

- [ ] **Step 7: Commit final focused fixes only when verification required changes**

Stage only the specific cart-related files changed during verification, then commit:

```powershell
git commit -m "test: verify shopping cart flow"
```
