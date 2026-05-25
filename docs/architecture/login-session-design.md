# 登录状态持久化设计

## 一、设计目标

当前阶段先实现轻量级登录态，不引入完整 Spring Security 鉴权链路。底层通过 Redisson 操作 Redis 保存登录态。目标是：

1. 用户登录后返回一个随机 token。
2. 服务端把 token 与用户 ID 的关系保存到 Redis。
3. 客户端后续通过 `Authorization: Bearer <token>` 识别当前用户。
4. 用户退出登录后，服务端删除 token，使登录态立即失效。

## 二、接口设计

### 1. 登录

`POST /api/users/login`

请求体：

```json
{
  "username": "test",
  "password": "password123"
}
```

成功后返回：

```json
{
  "token": "random-token",
  "user": {
    "id": 1,
    "username": "test"
  }
}
```

### 2. 当前用户

`GET /api/users/me`

请求头：

```text
Authorization: Bearer <token>
```

### 3. 退出登录

`POST /api/users/logout`

请求头：

```text
Authorization: Bearer <token>
```

## 三、Redis 设计

Redis key：

```text
mall:user:login:{token}
```

Redis value：

```text
userId
```

当前过期时间设置为 7 天，代码中通过 Redisson `RBucket` 写入 TTL。后续如果接入前端，可以根据“记住我”能力把普通登录和长期登录拆成不同 TTL。

## 四、关键规则

1. 密码只使用 BCrypt 校验，不保存明文。
2. 登录成功后更新 `ums_user.last_login_time`。
3. token 缺失返回 `401 missing login token`。
4. token 不存在或已退出返回 `401 invalid login token`。
5. 错误用户名或密码统一返回 `400 username or password incorrect`，避免泄露账号是否存在。
