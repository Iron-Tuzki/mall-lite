# 下单库存校验与锁库存设计

## 一、设计目标

下单前的库存校验与锁库存，是轻量电商系统进入交易链路后的第一个核心业务能力。

本阶段目标不是一次性完成完整订单系统，而是先把“用户选择 SKU 后，系统如何安全地判断库存是否足够，并把库存暂时锁住”这件事设计清楚。

第一版需要满足：

1. 支持按 SKU 锁定库存。
2. 并发下不能出现超卖。
3. 锁库存成功后，可用库存减少，锁定库存增加。
4. 锁库存失败时，不创建订单或订单明细。
5. 为后续订单取消释放库存、支付成功扣减库存、订单超时自动关闭预留扩展空间。

暂不处理：

1. Redis 预扣库存。
2. RabbitMQ 延迟关单。
3. 分布式事务。
4. 多仓库库存。
5. 秒杀场景下的极高并发削峰。

## 二、接口边界

### 1. 后台库存管理接口

当前已有 `InventoryAdminController`，它的职责是后台维护库存基础数据：

```text
POST   /api/admin/inventories
GET    /api/admin/inventories/{skuId}
PUT    /api/admin/inventories/{skuId}
DELETE /api/admin/inventories/{skuId}
```

这些接口面向后台管理，不应该承载用户下单时的锁库存逻辑。

### 2. 用户下单业务接口

下单前锁库存建议由订单模块统一编排，未来接口可以设计为：

```text
POST /api/orders
```

订单模块内部调用库存模块能力：

```text
OrderController
  -> OrderService.createOrder(...)
      -> 校验用户
      -> 校验收货地址
      -> 校验商品 SKU
      -> InventoryService.lockStock(...)
      -> 创建订单主表
      -> 创建订单明细表
      -> 返回订单结果
```

第一版不建议单独暴露用户侧的 `POST /api/inventories/lock` 接口。因为锁库存不是一个独立用户动作，而是下单流程中的一个内部步骤。

## 三、核心业务流程

### 1. 单个 SKU 下单流程

第一版可以先从单个 SKU 下单开始，流程如下：

```text
1. 用户提交下单请求
2. 查询并校验用户是否存在
3. 查询并校验收货地址是否属于该用户
4. 查询并校验 SKU 是否存在、是否上架
5. 调用库存模块锁定库存
6. 锁库存成功后创建订单主表
7. 创建订单明细表
8. 返回待支付订单信息
```

### 2. 多个 SKU 下单流程

后续支持购物车或多商品下单时，可以扩展为多个 SKU 批量锁库存：

```text
1. 校验所有 SKU 是否可售
2. 逐个锁定库存
3. 任意一个 SKU 锁定失败，则整体回滚
4. 全部锁定成功后创建订单和订单明细
```

多 SKU 场景必须在同一个数据库事务内完成，否则容易出现部分 SKU 已锁定、订单却创建失败的问题。

## 四、库存数据变化规则

库存表 `ims_inventory` 第一版使用两个核心字段：

| 字段 | 含义 | 变化场景 |
|---|---|---|
| `available_stock` | 可用库存 | 下单锁库存时减少，取消订单释放库存时增加 |
| `locked_stock` | 锁定库存 | 下单锁库存时增加，支付成功扣减库存或取消订单释放库存时减少 |

### 1. 下单锁库存

用户提交订单并通过基础校验后，库存变化为：

```text
available_stock = available_stock - quantity
locked_stock    = locked_stock + quantity
```

示例：

```text
下单前：available_stock = 100，locked_stock = 0
购买 2 件后：available_stock = 98，locked_stock = 2
```

### 2. 订单取消释放库存

如果订单未支付并被取消，库存变化为：

```text
available_stock = available_stock + quantity
locked_stock    = locked_stock - quantity
```

示例：

```text
取消前：available_stock = 98，locked_stock = 2
释放后：available_stock = 100，locked_stock = 0
```

### 3. 支付成功扣减库存

如果订单支付成功，锁定库存真正出库，库存变化为：

```text
locked_stock = locked_stock - quantity
```

注意：支付成功时不再减少 `available_stock`，因为可用库存已经在下单锁库存阶段减少过了。

示例：

```text
支付前：available_stock = 98，locked_stock = 2
支付后：available_stock = 98，locked_stock = 0
```

## 五、并发控制方案

### 1. 不推荐的做法

不推荐使用“先查询库存，再无条件更新库存”的方式：

```text
1. SELECT available_stock FROM ims_inventory WHERE sku_id = ?
2. Java 判断库存是否足够
3. UPDATE ims_inventory SET available_stock = available_stock - ?
```

这种方式在并发下容易超卖。

例如库存只有 1 件，两个请求同时查询都看到库存为 1，如果后续都执行无条件扣减，就可能卖出 2 件。

### 2. 推荐的做法：MySQL 条件更新

第一版推荐使用 MySQL 条件更新，把“判断库存是否足够”和“锁定库存”合并成一条 SQL：

```sql
UPDATE ims_inventory
SET available_stock = available_stock - #{quantity},
    locked_stock = locked_stock + #{quantity},
    version = version + 1,
    update_time = NOW()
WHERE sku_id = #{skuId}
  AND deleted = 0
  AND available_stock >= #{quantity}
```

执行结果判断：

1. 影响行数为 `1`：锁库存成功。
2. 影响行数为 `0`：库存不足、SKU 库存不存在，或库存记录已删除。

这种方式依赖 MySQL 单行更新的原子性，可以避免并发超卖。

### 3. 为什么仍然保留 `version`

`ims_inventory.version` 可以用于乐观锁扩展，但第一版锁库存不强制依赖它。

保留 `version` 的价值：

1. 便于观察库存变更次数。
2. 后续如果做后台库存调整，可以使用乐观锁避免覆盖更新。
3. 后续拆成独立库存服务时，可以作为并发控制字段继续演进。

## 六、事务边界

### 1. 下单事务

创建订单时，建议把以下操作放在同一个事务中：

```text
1. 锁定库存
2. 创建订单主表
3. 创建订单明细表
```

如果创建订单或订单明细失败，事务回滚后，库存锁定也应该回滚。

### 2. 为什么库存锁定要放在订单事务内

如果库存锁定成功后订单创建失败，但没有事务回滚，就会出现：

```text
库存已经被锁住，但是没有对应订单
```

这会导致库存被长期占用，影响后续用户购买。

### 3. 后续拆微服务时的变化

当前项目是单体多模块，可以使用本地事务保证一致性。

后续如果拆成订单服务和库存服务，就不能直接依赖本地事务，需要考虑：

1. 可靠消息最终一致性。
2. 分布式事务框架。
3. 库存冻结流水表。
4. 订单状态补偿任务。

第一版先不引入这些复杂方案。

## 七、异常处理

### 1. 常见失败场景

| 场景 | 建议错误码 | 建议提示 |
|---|---:|---|
| SKU 不存在或未上架 | 404 | sku not found |
| 库存记录不存在 | 404 | inventory not found |
| 购买数量小于 1 | 400 | quantity must be greater than 0 |
| 库存不足 | 400 | insufficient stock |
| 订单创建失败 | 500 或业务异常 | create order failed |

### 2. 库存不足的判断

条件更新影响行数为 `0` 时，需要进一步区分原因：

1. 如果库存记录不存在，返回 `inventory not found`。
2. 如果库存记录存在但可用库存不足，返回 `insufficient stock`。

第一版可以先用一次查询辅助判断，让接口提示更清晰。

## 八、代码结构建议

### 1. 库存模块

建议在 `mall-inventory` 中新增业务方法：

```text
InventoryService.lockStock(skuId, quantity)
InventoryService.releaseStock(skuId, quantity)
InventoryService.deductLockedStock(skuId, quantity)
```

第一阶段可以只实现：

```text
InventoryService.lockStock(skuId, quantity)
```

### 2. Mapper 层

建议在 `InventoryMapper` 中新增条件更新方法：

```text
int lockStock(Long skuId, Integer quantity)
```

该方法使用注解 SQL 或 XML SQL 都可以。当前项目还没有 XML Mapper，第一版可以使用注解 SQL，保持轻量。

### 3. 订单模块

后续在 `mall-order` 中补充：

```text
Order
OrderItem
OrderMapper
OrderItemMapper
OrderCreateRequest
OrderService
OrderController
```

订单创建时由 `OrderService` 调用 `InventoryService.lockStock(...)`。

## 九、关键代码注释约定

后续实现复杂逻辑时，代码注释遵循以下规则：

1. 类和接口继续使用中文 Java 标准注释说明职责。
2. 不给简单 getter、setter、普通赋值写无意义注释。
3. 对并发、事务、状态流转、库存数量变化写关键注释。
4. 注释重点解释“为什么这样做”，而不是重复“这行代码做了什么”。

例如锁库存方法中应说明：

```java
// 使用条件更新把“校验库存”和“锁定库存”合并为一次原子操作，避免并发下先查后改导致超卖。
```

订单创建方法中应说明：

```java
// 锁库存、创建订单、创建订单明细必须在同一个事务中完成，避免库存已锁定但订单创建失败。
```

## 十、后续扩展方向

### 1. 订单取消释放库存

订单取消时调用：

```text
InventoryService.releaseStock(skuId, quantity)
```

只允许待支付订单释放库存，避免已支付订单重复释放。

### 2. 支付成功扣减锁定库存

支付成功时调用：

```text
InventoryService.deductLockedStock(skuId, quantity)
```

只减少锁定库存，不再减少可用库存。

### 3. RabbitMQ 超时关单

后续可以在订单创建成功后发送延迟消息：

```text
订单创建成功
  -> 发送订单超时检查消息
  -> 到期后检查订单是否仍为待支付
  -> 如果未支付，则取消订单并释放库存
```

### 4. 库存流水表

当库存逻辑变复杂后，可以新增库存流水表记录每次库存变化：

```text
sku_id
order_id
change_type
change_quantity
before_available_stock
after_available_stock
before_locked_stock
after_locked_stock
```

库存流水有助于排查库存异常、重复释放、重复扣减等生产问题。

## 十一、第一版实现顺序

建议按以下顺序实现：

1. 在 `InventoryMapper` 增加 `lockStock` 条件更新方法。
2. 在 `InventoryService` 增加 `lockStock` 业务方法。
3. 编写库存锁定单元或集成测试，覆盖库存充足、库存不足、库存不存在。
4. 创建订单模块实体和 Mapper。
5. 实现 `POST /api/orders`，在订单创建事务中调用锁库存。
6. 增加订单创建集成测试，验证库存锁定和订单数据同时成功或同时回滚。

第一版只要把“创建订单时安全锁库存”打通，就已经覆盖了电商交易链路里非常核心的一块能力。
