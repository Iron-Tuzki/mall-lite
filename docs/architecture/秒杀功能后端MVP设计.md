# 秒杀功能后端 MVP 设计

## 一、设计目标

本阶段先实现秒杀功能的后端 MVP，目标是把“活动资格校验、秒杀库存预扣、用户限购、最终创建订单”这条链路打通。

第一版秒杀模块只负责秒杀活动入口和资格控制，不重写交易链路。抢购资格确认后，继续复用现有订单、库存、超时取消和支付流程。

需要满足：

1. 支持配置秒杀活动和活动 SKU。
2. 支持把活动库存预热到 Redis。
3. 抢购时使用 Redis 原子脚本预扣秒杀库存，避免活动库存超卖。
4. 支持同一用户的限购控制。
5. 支持请求幂等，避免同一秒杀请求重复扣减 Redis 库存。
6. 秒杀抢购成功后创建待支付订单，并使用秒杀价生成订单金额和明细价格。
7. Redis 预扣成功但订单创建失败时，同步补偿 Redis 库存和用户限购标记。

暂不处理：

1. RabbitMQ 异步排队下单。
2. 秒杀请求流水表和后台补偿任务。
3. 前端秒杀页面。
4. 活动自动预热定时任务。
5. 防刷、人机校验、接口隐藏 URL。

## 二、总体方案

采用“同步抢购 + Redis 原子预扣 + 复用现有下单链路”的方案。

核心边界：

```text
秒杀模块
  -> 校验活动、活动商品、时间窗口、限购规则
  -> Redis 原子预扣秒杀库存
  -> 调用订单模块创建待支付订单

订单模块
  -> 继续负责用户、地址、商品、真实库存锁定、订单幂等、订单明细、超时取消消息
```

这样第一版可以把秒杀核心能力做清楚，同时不破坏已有订单链路。

## 三、模块与代码边界

新增 Maven 模块：

```text
mall-backend/mall-seckill
```

职责：

1. 秒杀活动和活动 SKU 实体、DTO、VO、Mapper。
2. 秒杀活动查询。
3. 活动库存预热。
4. 秒杀下单资格校验。
5. Redis Lua 脚本执行和补偿。

`mall-app` 新增：

```text
com.tuzki.mall.seckill.controller.SeckillController
com.tuzki.mall.seckill.controller.SeckillAdminController
```

`mall-order` 扩展：

1. 保留现有 `createOrder(Long userId, OrderCreateRequest request)`。
2. 新增内部下单能力，允许调用方传入 SKU 价格覆盖信息。
3. 秒杀订单通过价格覆盖把活动价写入订单主表金额和订单明细快照。

第一版不新增秒杀订单表。订单仍落到：

```text
oms_order
oms_order_item
```

秒杀请求传给订单模块时，`request_id` 使用统一前缀：

```text
seckill:{seckillSkuId}:{requestId}
```

该前缀用于避免和普通订单请求幂等号混淆。

## 四、数据库设计

新增脚本：

```text
scripts/mysql/015_seckill_schema.sql
```

### 1. 秒杀活动表

```sql
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
```

### 2. 秒杀活动商品表

```sql
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

## 五、接口设计

### 1. 查询当前秒杀活动

```text
GET /api/seckill/activities/active
```

返回当前时间窗口内已启用的秒杀活动和活动 SKU。

### 2. 预热秒杀库存

```text
POST /api/admin/seckill/activities/{activityId}/preheat
```

后台手动触发，把活动下启用的活动 SKU 库存写入 Redis。

第一版不做自动预热任务，避免引入额外调度复杂度。

### 3. 秒杀下单

```text
POST /api/seckill/orders
```

请求体：

```json
{
  "seckillSkuId": 1,
  "requestId": "b7d4f6c8-xxxx-xxxx",
  "addressId": 2,
  "quantity": 1,
  "remark": "秒杀订单"
}
```

字段含义：

1. `seckillSkuId`：秒杀活动商品 ID。
2. `requestId`：秒杀请求幂等号，由前端生成。
3. `addressId`：收货地址 ID。
4. `quantity`：购买数量。
5. `remark`：用户备注。

返回现有 `OrderCreateVO`。

## 六、Redis 设计

### 1. 秒杀库存

```text
mall:seckill:stock:{seckillSkuId}
```

预热时写入 `sms_seckill_sku.stock_count`。

### 2. 用户限购标记

```text
mall:seckill:user:{seckillSkuId}:{userId}
```

抢购成功时写入本次购买数量。TTL 设置到活动结束后一小段时间。

### 3. 请求幂等标记

```text
mall:seckill:request:{seckillSkuId}:{userId}:{requestId}
```

用于避免同一用户对同一个活动商品重复提交同一个秒杀请求。

### 4. Lua 脚本返回语义

Lua 脚本一次性完成：

1. 判断请求幂等标记是否存在。
2. 判断用户当前已占用数量加本次购买数量是否超过限购。
3. 判断 Redis 秒杀库存是否充足。
4. 扣减 Redis 秒杀库存。
5. 累加用户限购占用数量。
6. 写入请求幂等标记。

建议返回码：

```text
0  预扣成功
1  重复请求
2  已达到限购
3  秒杀库存不足
4  秒杀库存未预热
```

## 七、秒杀下单流程

```text
接收秒杀下单请求
-> 从 Authorization 解析当前 userId
-> 查询 sms_seckill_sku
-> 查询 sms_seckill_activity
-> 校验活动启用、活动商品启用、未删除
-> 校验当前时间在活动开始和结束时间之间
-> 校验 quantity > 0
-> 校验 quantity <= limit_quantity
-> 执行 Redis Lua 脚本预扣活动库存并写入限购标记
-> 重复请求则不重复扣 Redis 库存，继续交给订单模块幂等查询原订单
-> 其他预扣失败场景按返回码抛出业务异常
-> 构造 OrderCreateRequest
-> requestId 使用 seckill:{seckillSkuId}:{requestId}
-> 调用订单模块带价格覆盖的下单能力
-> 创建订单成功，返回订单结果
-> 创建订单失败，补偿 Redis 秒杀库存和用户限购标记
```

订单创建时仍由订单模块完成真实库存锁定：

```text
InventoryService.lockStock(skuId, quantity)
```

秒杀预热库存不能大于真实可用库存。若 Redis 有库存但真实库存不足，订单模块会失败，秒杀模块同步补偿 Redis。

## 八、一致性策略

### 1. Redis 预扣成功，订单创建成功

正常返回订单。

### 2. Redis 预扣成功，订单创建失败

秒杀模块同步补偿：

```text
mall:seckill:stock:{seckillSkuId} + quantity
mall:seckill:user:{seckillSkuId}:{userId} - quantity，扣到 0 时删除
删除 mall:seckill:request:{seckillSkuId}:{userId}:{requestId}
```

### 3. Redis 库存和 MySQL 真实库存不一致

订单模块真实锁库存失败后，秒杀模块补偿 Redis。第一版通过文档约束预热库存不能超过真实可用库存。

### 4. 服务在 Redis 预扣后崩溃

第一版不引入请求流水和补偿任务。用户限购标记和请求幂等标记设置 TTL，到活动结束后自动释放。该场景可能导致极少量活动库存短暂占用，作为 MVP 风险接受。

## 九、异常处理

建议错误语义：

| 场景 | code | message |
| --- | ---: | --- |
| 秒杀活动商品不存在 | 404 | seckill sku not found |
| 秒杀活动不存在 | 404 | seckill activity not found |
| 活动未开始 | 400 | seckill activity not started |
| 活动已结束 | 400 | seckill activity ended |
| 活动或活动商品禁用 | 400 | seckill activity disabled |
| 购买数量小于 1 | 400 | quantity must be greater than 0 |
| 超过限购数量 | 400 | seckill purchase limit exceeded |
| 秒杀库存未预热 | 400 | seckill stock not preheated |
| 秒杀库存不足 | 400 | seckill stock sold out |
| 重复请求处理中 | 409 | seckill request is processing |
| 重复失败请求 | 409 | 流水记录中的失败原因 |

## 十、测试设计

### 1. 活动状态测试

覆盖活动未开始、已结束、禁用、活动商品禁用时不能抢购。

### 2. Redis 库存测试

覆盖未预热、库存不足、库存充足扣减成功。

### 3. 限购测试

同一用户超过 `limit_quantity` 时失败，不重复扣 Redis 库存。

### 4. 请求幂等测试

同一用户重复提交同一个 `requestId` 时，不重复扣 Redis 库存。

### 5. 补偿测试

模拟订单创建失败，验证 Redis 活动库存加回，用户限购标记和请求幂等标记被清理。

### 6. 价格覆盖测试

秒杀订单的订单总金额、实付金额、订单明细单价均使用 `seckill_price`，不使用 SKU 原价。

### 7. 并发测试

并发抢购时，成功数量不能超过 Redis 预热库存。

## 十一、实现顺序建议

1. 新增 `mall-seckill` Maven 模块并接入 `mall-app`。
2. 新增秒杀表结构脚本。
3. 新增秒杀活动和活动 SKU 实体、Mapper、DTO、VO。
4. 扩展订单模块价格覆盖创建订单能力，并补充订单价格覆盖测试。
5. 实现秒杀活动查询接口。
6. 实现秒杀库存预热接口。
7. 实现 Redis Lua 预扣和补偿逻辑。
8. 实现秒杀下单接口。
9. 补充活动状态、库存、限购、幂等、补偿、并发测试。

## 十二、后续扩展方向

1. 增加秒杀请求流水表，记录预扣、排队、下单成功、失败等状态。
2. 引入 RabbitMQ 异步排队下单，接口快速返回抢购结果或排队状态。
3. 增加后台补偿任务，扫描异常请求流水并修正 Redis 和订单状态。
4. 增加活动自动预热定时任务。
5. 增加接口隐藏 URL、验证码、风控限流和用户维度访问频控。
6. 前端增加秒杀列表、倒计时、抢购按钮状态和结果页。
## 十三、库存一致性补充

当前实现中，`sms_seckill_sku.stock_count` 表示秒杀活动商品的持久化剩余活动库存，Redis 的 `mall:seckill:stock:{seckillSkuId}` 表示活动期间用于高并发预扣的热库存。

### 1. 预热策略

活动预热支持后台手动触发和定时任务自动触发。定时任务会扫描已启用、未删除、未结束，并且开始时间落在预热窗口内的活动。

预热写 Redis 时采用“存在则只续期，不存在才初始化”的策略：

1. 如果 `mall:seckill:stock:{seckillSkuId}` 不存在，则使用 `sms_seckill_sku.stock_count` 初始化 Redis 活动库存。
2. 如果 Redis 库存 key 已存在，则不覆盖当前 Redis 库存，只刷新 TTL 到活动结束后一小段时间。

这样可以避免定时预热任务在活动进行中把已经被抢购扣减过的 Redis 库存重新刷回数据库旧值。

### 2. 成功下单后的活动库存扣减

秒杀请求的成功路径调整为：

```text
Redis Lua 原子预扣成功
-> 调用订单模块创建待支付订单并锁定真实库存
-> 订单创建成功后，原子扣减 sms_seckill_sku.stock_count
-> 返回订单创建结果
```

`sms_seckill_sku.stock_count` 使用条件更新扣减：

```sql
UPDATE sms_seckill_sku
   SET stock_count = stock_count - #{quantity},
       update_time = NOW()
 WHERE id = #{seckillSkuId}
   AND status = 1
   AND deleted = 0
   AND stock_count >= #{quantity}
```

前面流程已经做过幂等处理，如果该更新影响行数不是 1，说明数据库侧活动库存不足或活动商品不可用，本次下单按 `seckill stock sold out` 失败处理，并触发 Redis 预扣补偿。

### 3. 重复请求与补偿

同一用户、同一活动商品、同一 `requestId` 重复请求时，Redis Lua 返回重复请求，不再重复扣减 Redis 活动库存，也不会重复扣减 `sms_seckill_sku.stock_count`，后续由订单模块幂等返回原订单。

如果 Redis 预扣成功后，订单创建、真实库存锁定或活动库存持久扣减任一环节失败，当前事务回滚订单侧变更，并同步补偿 Redis 活动库存、用户限购占用和请求幂等标记。
## 十四、秒杀请求流水表

新增 `sms_seckill_request` 作为秒杀请求的持久化流水和幂等兜底表。它记录同一用户、同一活动商品、同一 `requestId` 的处理状态，后续补偿任务、MQ 异步下单和问题排查都以该表为抓手。

### 1. 幂等边界

流水表增加唯一键：

```sql
UNIQUE KEY uk_user_sku_request (user_id, seckill_sku_id, request_id)
```

第一版采用“双层幂等”：

1. `sms_seckill_request` 负责持久化幂等，解决 Redis key 过期、服务重启、补偿排查等问题。
2. Redis Lua 中的 `mall:seckill:request:{seckillSkuId}:{userId}:{requestId}` 继续保留，负责高并发瞬间的原子防重和快速拒绝。

因此当前不删除 Lua 脚本里的幂等判断。后续如果切到完整 MQ 异步状态机，再评估是否弱化 Redis 请求幂等 key。

### 2. 状态流转

```text
10 INIT           请求已接收
20 PRE_DEDUCTED  Redis 预扣成功
30 ORDER_CREATED 订单创建成功
40 FAILED         失败
50 COMPENSATED    Redis 预扣已补偿
```

为了保留失败和补偿记录，流水写入和状态推进使用独立事务。主下单事务失败回滚时，流水仍会保留最终失败或已补偿状态。

### 3. 重复请求处理

当 `INSERT IGNORE` 返回 0 时，说明同一用户、同一活动商品、同一 `requestId` 的流水已经存在。此时使用 `SELECT ... FOR UPDATE` 当前读查询已有流水，确保重复请求基于最新已提交状态做决策。

重复请求按状态处理：

1. `ORDER_CREATED` 且存在 `order_id`：走订单幂等查询，返回原订单。
2. `INIT` 或 `PRE_DEDUCTED`：说明原请求仍在处理，返回 `seckill request is processing`。
3. `FAILED` 或 `COMPENSATED`：返回流水记录中的失败原因。

`FOR UPDATE` 只能保证读取最新已提交流水并在并发更新时等待行锁释放，不代表一定等待原秒杀请求完整下单结束。因此处理中状态仍然需要显式返回或由上层后续扩展短轮询。

### 4. 后台补偿任务

第一版后台补偿任务只处理长时间停留在 `PRE_DEDUCTED` 状态的流水。该状态表示 Redis 已经预扣成功，但请求没有继续推进到订单创建成功、失败或已补偿，可能是服务进程中断或主链路异常导致。

任务配置：

```yaml
mall:
  seckill:
    compensation:
      enabled: true
      fixed-delay-ms: 60000
      timeout-seconds: 120
      batch-size: 100
      max-retry-count: 3
```

执行流程：

1. `SeckillCompensationTask` 使用分布式锁 `mall:seckill:compensation` 防止多实例重复扫描。
2. `SeckillCompensationService` 扫描 `status = PRE_DEDUCTED`、`update_time <= now - timeoutSeconds`、`retry_count < maxRetryCount` 的流水。
3. 对每条流水调用 Redis 补偿脚本，回补活动库存、扣减用户限购占用并删除请求幂等 key。
4. 补偿成功后使用 CAS 条件 `status = PRE_DEDUCTED` 将流水推进到 `COMPENSATED`。
5. 补偿失败时只增加 `retry_count` 并记录失败原因，下一轮任务继续处理。

Redis 补偿脚本增加请求幂等 key 判断：只有 `mall:seckill:request:{seckillSkuId}:{userId}:{requestId}` 存在时才执行回补。这样即使任务在 Redis 已补偿但数据库还未标记完成时中断，下一轮重复执行也不会把库存重复加回去。

当前任务不处理 `INIT` 状态，因为 `INIT` 不能证明 Redis 已经预扣成功。后续如果需要清理长期初始化状态，可以单独增加“初始化超时失败标记”任务。
