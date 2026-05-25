# 下单请求幂等设计

## 一、设计目标

下单接口除了要防止库存超卖，还需要防止同一次下单请求被重复提交后创建多张订单。

典型重复请求来源：

1. 前端按钮重复点击。
2. 前端请求超时后自动重试。
3. 网关或客户端网络抖动导致重复发送。
4. 用户刷新页面后重复提交同一个确认订单请求。

当前目标是保证：

```text
同一个登录用户 + requestId 只创建一张订单
```

用户 ID 不再由请求体传入，而是通过 `Authorization: Bearer <token>` 解析登录态得到。

## 二、接口字段

`POST /api/orders`

请求头：

```text
Authorization: Bearer <token>
```

请求体包含 `requestId` 和 `items`：

```json
{
  "requestId": "b7d4f6c8-xxxx-xxxx",
  "addressId": 2,
  "items": [
    {
      "skuId": 1001,
      "quantity": 2
    },
    {
      "skuId": 1002,
      "quantity": 1
    }
  ],
  "remark": "please ship soon"
}
```

字段含义：

1. `requestId`：下单请求幂等号，由前端在用户提交订单前生成。
2. `addressId`：当前登录用户名下的收货地址 ID。
3. `items`：订单明细列表，一条记录表示一个 SKU 及其购买数量。
4. 同一登录用户重复提交同一个 `requestId`，后端返回第一次创建的订单。
5. 不同用户可以使用相同 `requestId`，互不影响。

## 三、数据库约束

`oms_order` 保存下单请求幂等号：

```sql
request_id VARCHAR(64) NULL COMMENT '下单请求幂等号'
```

唯一索引：

```sql
UNIQUE KEY uk_user_request (user_id, request_id)
```

数据库唯一索引是最终兜底。即使两个请求并发通过了应用层查询，也只有一个请求能插入成功。

## 四、核心流程

```text
接收下单请求
-> 从 Authorization 解析 token
-> 从 Redis 登录态读取当前 userId
-> 根据 userId + requestId 查询是否已有订单
-> 如果已有，直接返回已有订单
-> 校验用户、地址
-> 校验 items 非空
-> 校验同一请求中不能重复传入相同 skuId
-> 逐个校验 SKU、商品状态
-> 逐个锁定库存
-> 汇总订单总金额
-> 插入订单主表，写入 userId 和 requestId
-> 如果唯一索引冲突，释放本次已锁定库存并返回已有订单
-> 插入订单明细
-> 返回订单创建结果
```

## 五、错误处理

| 场景 | code | message |
| --- | --- | --- |
| 未传 `Authorization` | `401` | `missing login token` |
| token 为空 | `401` | `missing login token` |
| token 不存在或已退出 | `401` | `invalid login token` |
| 收货地址不属于当前用户 | `404` | `address not found` |

## 六、和防超卖的区别

| 能力 | 解决的问题 | 当前实现 |
| --- | --- | --- |
| 防超卖 | 多个请求不能把库存扣成负数 | `ims_inventory` 条件更新 |
| 下单幂等 | 同一个请求不能创建多张订单 | `user_id + request_id` 唯一索引 |
| 支付回调幂等 | 同一支付结果不能重复扣锁定库存 | `payment_no + status` 条件更新 |
