# 秒杀接口 Redis 滑动窗口限流笔记

## 一、脚本解决的问题

`SLIDING_WINDOW_SCRIPT` 用来做 **Redis 滑动窗口限流**。

它的目标是判断：

> 在最近 N 秒内，某个用户或某个 IP 的秒杀请求次数是否超过阈值。

当前秒杀接口有两个限流维度：

```text
mall:seckill:rate:user:{userId}
mall:seckill:rate:ip:{clientIp}
```

Redis 数据结构使用 **ZSet**：

1. `score`：请求发生时间，毫秒时间戳。
2. `member`：本次请求的唯一标识，当前代码是 `now + ":" + UUID`。
3. ZSet 里保存的是最近一段时间内的多条请求记录。

## 二、Lua 脚本原文

```lua
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
local current = redis.call('ZCARD', KEYS[1])
if current >= tonumber(ARGV[3]) then
    return 0
end
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[4])
redis.call('EXPIRE', KEYS[1], ARGV[5])
return 1
```

## 三、参数含义

| 参数 | 含义 | 示例 |
|---|---|---|
| `KEYS[1]` | 当前要限流的 Redis key | `mall:seckill:rate:user:900001` |
| `ARGV[1]` | 窗口起点时间戳 | `now - windowMillis` |
| `ARGV[2]` | 当前请求时间戳 | `now` |
| `ARGV[3]` | 最大允许请求次数 | `user-limit = 3` |
| `ARGV[4]` | 当前请求唯一 member | `now + ":" + UUID` |
| `ARGV[5]` | Redis key 过期时间 | `windowSeconds + 1` |

## 四、逐行解释

### 1. 删除窗口外的旧请求

```lua
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
```

删除所有 `score <= 窗口起点` 的记录。

比如当前时间是第 10 秒，窗口大小是 5 秒，则只保留：

```text
5s ~ 10s
```

更早的请求已经不影响当前限流判断，可以删除。

### 2. 统计窗口内请求数

```lua
local current = redis.call('ZCARD', KEYS[1])
```

`ZCARD` 返回当前 ZSet 里的元素数量。因为旧数据刚刚被清理过，所以这里的 `current` 就是：

```text
最近 N 秒内已经发生的请求次数
```

### 3. 判断是否超限

```lua
if current >= tonumber(ARGV[3]) then
    return 0
end
```

如果当前窗口内请求数已经达到阈值，就拒绝本次请求。

这里使用 `>=`。比如用户限制 5 秒 3 次：

```text
current = 3
limit = 3
```

说明窗口里已经有 3 次了，第 4 次应该被拒绝，所以返回 `0`。

### 4. 写入本次请求

```lua
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[4])
```

如果没有超限，就把本次请求写入 ZSet：

```text
score = 当前时间戳
member = 当前请求唯一值
```

### 5. 设置过期时间

```lua
redis.call('EXPIRE', KEYS[1], ARGV[5])
```

每次允许通过时刷新 TTL。这样如果这个用户或 IP 不再请求，对应防刷 key 会自动消失。

### 6. 返回允许

```lua
return 1
```

返回 `1` 表示允许请求继续执行。Java 里对应判断：

```java
return result != null && result == ALLOWED;
```

其中：

```java
ALLOWED = 1L
```

## 五、为什么要用 Lua

这几步必须作为一个原子操作执行：

```text
清理旧数据
-> 统计当前数量
-> 判断是否超限
-> 写入本次请求
-> 设置 TTL
```

如果不用 Lua，而是在 Java 里分多条 Redis 命令执行，高并发下可能出现并发穿透：

```text
请求 A 查到 current = 2
请求 B 也查到 current = 2
A 通过
B 也通过
```

Lua 脚本在 Redis 内部一次性执行，中间不会被其他 Redis 命令插入，所以判断和写入是一体的。

## 六、为什么是滑动窗口

固定窗口是：

```text
0~5 秒一个窗口
5~10 秒一个窗口
```

它容易出现边界突刺，比如：

```text
第 4.9 秒发 3 次
第 5.1 秒再发 3 次
```

从固定窗口看都没超限，但真实 0.2 秒内已经发了 6 次。

滑动窗口是：

```text
每次请求都看“当前时间往前 N 秒”
```

所以更平滑、更准确，也更适合秒杀防刷、登录防刷、验证码防刷这类高风险接口。

## 七、SeckillRateLimitService 源码

以下代码逻辑与当前 `SeckillRateLimitService` 保持一致，JavaDoc 注释整理为可读中文。

```java
package com.tuzki.mall.seckill.ratelimit;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 秒杀接口 Redis 滑动窗口防刷服务，负责按用户和 IP 维度拦截异常高频请求。
 */
@Service
public class SeckillRateLimitService {

    private static final long ALLOWED = 1L;

    private static final String SLIDING_WINDOW_SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
            local current = redis.call('ZCARD', KEYS[1])
            if current >= tonumber(ARGV[3]) then
                return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[2], ARGV[4])
            redis.call('EXPIRE', KEYS[1], ARGV[5])
            return 1
            """;

    private final RedissonClient redissonClient;

    private final SeckillRateLimitProperties properties;

    public SeckillRateLimitService(RedissonClient redissonClient, SeckillRateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    public boolean isAllowed(Long userId, String clientIp) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return true;
        }
        int windowMillis = normalizeWindowSeconds() * 1000;
        long now = System.currentTimeMillis();
        if (!allow(userKey(userId), now, windowMillis, properties.getUserLimit())) {
            return false;
        }
        return allow(ipKey(clientIp), now, windowMillis, properties.getIpLimit());
    }

    public String userKey(Long userId) {
        return "mall:seckill:rate:user:" + userId;
    }

    public String ipKey(String clientIp) {
        return "mall:seckill:rate:ip:" + normalizeClientIp(clientIp);
    }

    private boolean allow(String key, long now, int windowMillis, Integer limit) {
        if (limit == null || limit <= 0) {
            return true;
        }
        Long result = script().eval(
                RScript.Mode.READ_WRITE,
                SLIDING_WINDOW_SCRIPT,
                RScript.ReturnType.LONG,
                List.of(key),
                String.valueOf(now - windowMillis),
                String.valueOf(now),
                String.valueOf(limit),
                now + ":" + UUID.randomUUID(),
                String.valueOf(normalizeWindowSeconds() + 1)
        );
        return result != null && result == ALLOWED;
    }

    private int normalizeWindowSeconds() {
        Integer windowSeconds = properties.getWindowSeconds();
        if (windowSeconds == null || windowSeconds <= 0) {
            return 1;
        }
        return windowSeconds;
    }

    private String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "unknown";
        }
        return clientIp.trim();
    }

    private RScript script() {
        return redissonClient.getScript(StringCodec.INSTANCE);
    }
}
```

## 八、生产注意点

1. **限流阈值需要按活动热度调整**：默认 `5 秒 3 次/用户`、`5 秒 10 次/IP` 适合本项目 MVP，不一定适合真实大促。
2. **IP 维度可能误伤 NAT 出口用户**：公司、学校、公共网络下多个真实用户可能共享同一个出口 IP。
3. **Redis ZSet 会占用更多内存**：相比固定窗口 `INCR`，滑动窗口更准确，但每次允许请求都会保存一条记录。
4. **Sentinel 和 Redis 防刷职责不同**：Sentinel 保护服务整体 QPS，Redis 防刷保护单用户和单 IP 高频请求。
5. **限流失败不应进入库存预扣**：当前实现放在 Controller 入口，命中防刷后不会写请求流水，也不会扣 Redis 库存。
