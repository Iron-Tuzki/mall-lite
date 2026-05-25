# 用户与收货地址表设计

## 一、设计目标

本阶段只设计用户域的两张基础表：

1. `ums_user`：用户基础信息表。
2. `ums_address`：用户收货地址表。

这两张表先支撑轻量电商系统的用户登录、用户识别、收货地址管理和后续下单流程。角色权限、会员等级、第三方登录、登录日志等扩展能力暂不纳入第一版。

## 二、命名约定

### 1. 表名前缀

用户域统一使用 `ums` 前缀，表示 User Management System。

| 表名 | 说明 |
| --- | --- |
| `ums_user` | 用户基础信息表 |
| `ums_address` | 用户收货地址表 |

### 2. 通用字段

两张表统一保留以下通用字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `bigint unsigned` | 主键 ID |
| `create_time` | `datetime` | 创建时间 |
| `update_time` | `datetime` | 更新时间 |
| `deleted` | `tinyint unsigned` | 逻辑删除标记，0 未删除，1 已删除 |

## 三、`ums_user` 用户表

### 1. 表作用

`ums_user` 用于保存用户的基础账号信息，是后续订单、地址、支付等业务关联用户的主表。

第一版只保留轻量电商最常用字段：

1. 用户名。
2. 密码摘要。
3. 昵称。
4. 手机号。
5. 邮箱。
6. 头像。
7. 用户状态。
8. 最近登录时间。

### 2. 字段设计

| 字段 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint unsigned` | 是 | 自增 | 用户 ID |
| `username` | `varchar(64)` | 是 | 无 | 用户名，唯一 |
| `password` | `varchar(255)` | 是 | 无 | 密码摘要，不保存明文密码 |
| `nickname` | `varchar(64)` | 否 | `NULL` | 用户昵称 |
| `phone` | `varchar(20)` | 否 | `NULL` | 手机号，唯一 |
| `email` | `varchar(128)` | 否 | `NULL` | 邮箱 |
| `avatar_url` | `varchar(255)` | 否 | `NULL` | 头像地址 |
| `status` | `tinyint unsigned` | 是 | `1` | 用户状态：1 正常，0 禁用 |
| `last_login_time` | `datetime` | 否 | `NULL` | 最近登录时间 |
| `create_time` | `datetime` | 是 | 当前时间 | 创建时间 |
| `update_time` | `datetime` | 是 | 当前时间 | 更新时间 |
| `deleted` | `tinyint unsigned` | 是 | `0` | 逻辑删除标记：0 未删除，1 已删除 |

### 3. 索引设计

| 索引名 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `uk_username` | `username` | 唯一索引 | 保证用户名唯一 |
| `uk_phone` | `phone` | 唯一索引 | 保证手机号唯一 |
| `idx_status` | `status` | 普通索引 | 支持按用户状态筛选 |

### 4. 生产注意点

1. **密码不能明文保存**，后续业务实现时应保存 BCrypt、Argon2 或其他安全哈希结果。
2. 手机号字段允许为空。MySQL 唯一索引允许多个 `NULL`，适合“未绑定手机号”的用户。
3. 逻辑删除后，唯一索引仍然会占用用户名和手机号。第一版先保持简单，后续如果要支持删除后重新注册，可以再引入 `deleted` 参与唯一约束或使用归档策略。
4. `status = 0` 表示禁用用户，登录和下单前都应校验用户状态。

## 四、`ums_address` 收货地址表

### 1. 表作用

`ums_address` 用于保存用户的收货地址。一个用户可以有多个收货地址，后续订单创建时会从地址表读取收货人、手机号和详细地址信息。

### 2. 字段设计

| 字段 | 类型 | 是否必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| `id` | `bigint unsigned` | 是 | 自增 | 地址 ID |
| `user_id` | `bigint unsigned` | 是 | 无 | 用户 ID |
| `receiver_name` | `varchar(64)` | 是 | 无 | 收货人姓名 |
| `receiver_phone` | `varchar(20)` | 是 | 无 | 收货人手机号 |
| `province` | `varchar(64)` | 是 | 无 | 省 |
| `city` | `varchar(64)` | 是 | 无 | 市 |
| `district` | `varchar(64)` | 是 | 无 | 区/县 |
| `detail_address` | `varchar(255)` | 是 | 无 | 详细地址 |
| `postal_code` | `varchar(20)` | 否 | `NULL` | 邮政编码 |
| `default_flag` | `tinyint unsigned` | 是 | `0` | 是否默认地址：1 是，0 否 |
| `create_time` | `datetime` | 是 | 当前时间 | 创建时间 |
| `update_time` | `datetime` | 是 | 当前时间 | 更新时间 |
| `deleted` | `tinyint unsigned` | 是 | `0` | 逻辑删除标记：0 未删除，1 已删除 |

### 3. 索引设计

| 索引名 | 字段 | 类型 | 说明 |
| --- | --- | --- | --- |
| `idx_user_id` | `user_id` | 普通索引 | 查询用户地址列表 |
| `idx_user_default` | `user_id, default_flag` | 普通索引 | 查询用户默认地址 |

### 4. 默认地址规则

默认地址使用 `default_flag` 表示：

1. `1`：默认地址。
2. `0`：非默认地址。

第一版由业务层保证同一个用户只有一个默认地址。典型流程是：

1. 开启事务。
2. 将当前用户其他地址的 `default_flag` 更新为 `0`。
3. 将目标地址的 `default_flag` 更新为 `1`。
4. 提交事务。

### 5. 生产注意点

1. 不建议对 `receiver_phone` 建唯一索引，因为多个地址可能使用同一个收货手机号。
2. 订单创建后，应将地址信息快照保存到订单表中。否则用户修改地址会影响历史订单展示。
3. 删除用户地址建议使用逻辑删除，避免历史订单关联数据难以追踪。

## 五、表关系

`ums_user` 与 `ums_address` 是一对多关系：

```text
ums_user.id 1 ---- n ums_address.user_id
```

第一版 SQL 中不强制添加外键，原因是：

1. 电商系统后续可能会按业务拆分服务，强外键会增加拆分成本。
2. 高并发写入场景下，外键约束可能带来额外开销。
3. 数据一致性优先通过业务层、事务和测试保证。

如果只是单体学习项目，也可以后续按需要补外键约束。
