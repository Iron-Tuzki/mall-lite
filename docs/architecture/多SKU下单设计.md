# 多 SKU 下单设计

## 一、设计目标

下单接口从单个 SKU 扩展为多个 SKU，真正表达一张订单包含多条订单明细的关系：

```text
oms_order 1 : N oms_order_item
```

当前版本暂不引入购物车、优惠券和运费计算，只解决一张订单同时购买多个 SKU 的核心链路。

## 二、请求结构

`POST /api/orders`

请求头：

```text
Authorization: Bearer <token>
```

请求体：

```json
{
  "requestId": "REQ-001",
  "addressId": 900001,
  "items": [
    {
      "skuId": 900001,
      "quantity": 2
    },
    {
      "skuId": 900002,
      "quantity": 3
    }
  ],
  "remark": "multi sku order test"
}
```

说明：

1. `userId` 不再由前端传入，后端从登录态 token 中解析当前用户。
2. `items` 不能为空。
3. 每个明细都必须有 `skuId` 和大于 0 的 `quantity`。
4. 同一个请求中不允许重复传入相同 `skuId`。

## 三、核心流程

```text
解析 Authorization token 得到 userId
-> 检查 userId + requestId 是否已有订单
-> 校验用户和收货地址
-> 校验 items
-> 校验重复 skuId
-> 查询 SKU 和商品状态
-> 按明细逐个锁库存
-> 汇总订单总金额
-> 创建 oms_order
-> 创建多条 oms_order_item
-> 返回订单创建结果
```

## 四、事务和回滚

多 SKU 下单必须放在同一个数据库事务中：

1. 任意 SKU 库存不足，整个下单失败。
2. 已锁库存、订单主表、订单明细要一起提交或一起回滚。
3. 并发重复请求触发 `user_id + request_id` 唯一索引冲突时，需要主动释放本次已经锁定的库存，再返回已有订单。

## 五、订单金额计算

订单总金额来自所有明细小计之和：

```text
order.total_amount = sum(sku.price * quantity)
order.pay_amount = order.total_amount
```

订单明细会保存下单快照：

1. 商品 ID、SKU ID。
2. 商品名称、SKU 名称、SKU 编码。
3. 规格、主图。
4. 下单时单价、购买数量、明细小计。

## 六、当前取舍

1. 第一版拒绝重复 SKU，不在后端自动合并数量。
2. 第一版逐个 SKU 锁库存，事务保证整体一致性。
3. 后续如果加入购物车、优惠券、促销分摊，可以在 `items` 上继续扩展价格来源和优惠明细。
