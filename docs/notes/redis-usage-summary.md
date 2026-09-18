# mall-lite 项目 Redis 使用汇总

> 本文基于当前项目生产源码与 `application.yml` 整理，统计范围不包含测试代码、历史设计稿和实现计划。分析基线日期为 **2026-09-01**。

## 一、整体结论

### 1. Redis 在项目中的定位

Redis 在本项目中不仅承担普通缓存，还承担了以下五类职责：

1. **读性能优化**：商品详情、收藏状态、热门商品榜单、浏览足迹。
2. **临时状态存储**：登录会话、购物车实时状态、签到记录。
3. **高并发控制**：秒杀库存预扣、限购计数、请求幂等、防刷限流。
4. **原子操作载体**：通过 Lua 脚本把多步校验和更新合并为一次原子执行。
5. **分布式协调**：通过 Redisson `RLock` 控制定时任务单实例执行，并保护热门商品缓存重建。

当前项目使用 `redisson-spring-boot-starter`，版本为 **4.4.0**。生产代码中共有 **11 个类直接调用 Redisson**，另有 **5 个定时任务**通过统一的 Redis 分布式锁切面间接使用 Redis。

### 2. 使用到的数据结构

| 数据结构或能力 | 项目中的主要场景 | 核心原因 |
| --- | --- | --- |
| **String** | 登录会话、商品详情 JSON、空值标记、缓存加载标记、浏览去重、消费幂等、秒杀库存/限购/请求幂等 | 适合保存单值、计数器和存在性标记，支持 TTL、`SET NX`、`INCRBY/DECRBY` |
| **Hash** | 用户购物车 | 一个用户一个 Key、一个 SKU 一个 Field，适合局部修改和一次读取整车数据 |
| **Set** | 用户商品收藏 | 天然去重，适合成员存在判断、添加和删除 |
| **ZSet** | 商品足迹、热门商品小时桶和榜单、秒杀滑动窗口限流 | 同时保存唯一成员和可排序分值，适合时间排序、热度排序、范围裁剪 |
| **Bitmap** | 用户按月签到 | 每天只占 1 bit，空间小，按天判断和月度计数方便 |
| **RLock** | 热门详情缓存重建、多个定时任务互斥 | 跨应用实例互斥，避免缓存击穿或任务重复执行 |
| **Lua 脚本** | 购物车变更/回滚、秒杀预扣/补偿、滑动窗口限流 | 将“读取、判断、更新、过期”组合成 Redis 端原子操作 |

项目当前**没有使用** Redis List、Stream、HyperLogLog、Geo 等数据结构。

## 二、Redis 基础配置

### 1. 连接配置

配置文件：`mall-backend/mall-app/src/main/resources/application.yml`

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3s
```

当前配置表现为单节点本地 Redis，未在项目配置中看到 Sentinel 或 Cluster 拓扑配置。

### 2. 引入 Redis 的模块

以下 Maven 模块直接引入了 Redisson：

- `mall-app`：分布式锁、秒杀限流。
- `mall-user`：登录会话、用户签到。
- `mall-product`：商品详情、收藏、足迹、热门榜单。
- `mall-cart`：购物车实时缓存。
- `mall-seckill`：秒杀库存预热、预扣和补偿。

## 三、Redis 使用点总览

| 业务场景 | Key 示例或模式 | 数据结构 | 过期策略 | Redis 中保存的内容 |
| --- | --- | --- | --- | --- |
| 登录会话 | `mall:user:login:{token}` | String | 1 天 | 用户 ID |
| 普通商品详情 | `mall:product:detail:{productId}` | String | 正常值 30～35 分钟；空值 5 分钟 | 商品详情 JSON 或 `__NULL__` |
| 热门商品详情 | `mall:product:hot:detail:{productId}` | String | 正常值 30～35 分钟；空值 5 分钟 | 热门商品详情 JSON 或 `__NULL__` |
| 热门详情重建锁 | `mall:product:hot:detail:lock:{productId}` | RLock | 显式租约 10 秒 | Redisson 锁状态 |
| 商品收藏 | `mall:user:favorites:{userId}` | Set | 24～24.5 小时 | 已收藏商品 ID |
| 收藏加载标记 | `mall:user:favorites:loaded:{userId}` | String | 24～24.5 小时 | 固定值 `1` |
| 浏览足迹 | `mall:user:footprints:{userId}` | ZSet | 90 天，最多 100 条 | member=商品 ID，score=浏览时间戳 |
| 浏览热度去重 | `mall:product:hot:view:dedup:{actor}:{productId}` | String | 5 分钟 | 固定值 `1` |
| 热度事件幂等 | `mall:product:hot:event:processed:{eventId}` | String | 48 小时 | 固定值 `1` |
| 商品热度小时桶 | `mall:product:hot:hour:{yyyyMMddHH}` | ZSet | 48 小时 | member=商品 ID，score=小时内累计热度 |
| 首页热门榜单 | `mall:product:hot:homepage` | ZSet | 15 分钟 | member=商品 ID，score=加权热度 |
| 签到 | `mall:user:sign:{userId}:{yyyyMM}` | Bitmap | 400 天 | 当月每天的签到位 |
| 购物车 | `mall:cart:user:{userId}` | Hash | 24 小时 | field=SKU ID，value=数量、版本、墓碑 JSON |
| 购物车加载标记 | `mall:cart:loaded:{userId}` | String | 24 小时 | 固定值 `1` |
| 秒杀库存 | `mall:seckill:stock:{seckillSkuId}` | String 计数器 | 活动结束时间再加 5 分钟 | 剩余可抢库存 |
| 秒杀用户限购 | `mall:seckill:user:{seckillSkuId}:{userId}` | String 计数器 | 活动结束时间再加 5 分钟 | 用户已预扣数量 |
| 秒杀请求幂等 | `mall:seckill:request:{seckillSkuId}:{userId}:{requestId}` | String | 活动结束时间再加 5 分钟 | 固定值 `1` |
| 秒杀用户限流 | `mall:seckill:rate:user:{userId}` | ZSet | 窗口时间加 1 秒 | 用户在窗口内的请求记录 |
| 秒杀 IP 限流 | `mall:seckill:rate:ip:{ip}` | ZSet | 窗口时间加 1 秒 | IP 在窗口内的请求记录 |
| 定时任务互斥 | 各任务固定 Lock Key | RLock | Redisson 锁生命周期 | 锁持有状态 |

## 四、各业务场景为什么这样设计

### 1. 登录会话：String

实现类：`RedisLoginSessionService`

登录成功后生成随机 Token，把 Token 拼到 Key 中，把用户 ID 保存为 String 值：

```text
mall:user:login:{token} -> userId
```

选择 String 的原因：

1. Token 与用户 ID 是简单的一对一映射，不需要复杂结构。
2. 登录校验只需要通过 Token 做一次 O(1) 查询。
3. Redis TTL 可以让会话在 1 天后自动失效，无需额外清理任务。
4. 退出登录时直接删除 Key 即可。

当前实现是**固定过期会话**，读取会话时不会续期，不属于滑动过期。

### 2. 商品详情缓存：String

实现类：

- `ProductDetailCacheService`
- `ProductHotDetailCacheService`
- `ProductCacheInvalidationService`

商品详情对象先序列化为 JSON，再整体写入 String。之所以不使用 Hash，是因为读取商品详情时通常需要完整对象，代码中也没有对单个字段做局部更新；整体 JSON 的读写路径更简单。

该场景同时使用了三种缓存保护手段：

1. **空值缓存**：不存在的商品写入 `__NULL__`，保存 5 分钟，避免同一个非法商品 ID 持续穿透到 MySQL。
2. **随机 TTL**：正常缓存 TTL 为 30 分钟，再随机增加 0～5 分钟，降低大量 Key 同时失效造成缓存雪崩的概率。
3. **延迟双删**：后台修改商品并提交事务后，立即删除普通详情和热门详情缓存，再延迟约 500ms 删除一次，降低并发回源把旧数据重新写入缓存的概率。

热门商品详情额外使用 `RLock` 做**互斥重建**：

1. 缓存未命中后按商品 ID 抢锁。
2. 抢到锁后再次检查缓存。
3. 仍未命中时，只有锁持有者访问数据库并重建缓存。
4. 未抢到锁的请求短暂重试读取缓存，仍未完成则返回 503。

这样可以防止热门 Key 失效瞬间，大量请求同时打到数据库形成**缓存击穿**。

### 3. 商品收藏：Set + String 加载标记

实现类：`ProductFavoriteCacheService`

每个用户使用一个 Set 保存收藏商品 ID：

```text
mall:user:favorites:{userId} = {productId1, productId2, ...}
```

选择 Set 的原因：

1. 收藏商品 ID 天然需要去重。
2. `SISMEMBER` 适合快速判断某个商品是否已收藏。
3. `SADD`、`SREM` 与收藏、取消收藏的业务动作直接对应。
4. 业务不关心收藏顺序，因此不需要 List 或 ZSet。

额外的 `loaded` String 标记用于区分两种状态：

- 收藏集合确实为空。
- Redis 中还没有从 MySQL 加载过收藏数据。

因为空 Set 在 Redis 中不会保留一个可供判断的实体 Key，仅检查 Set 是否存在无法可靠区分“空集合”和“缓存未加载”。因此使用单独的 loaded Key 是合理的负缓存设计。

### 4. 商品浏览足迹：ZSet

实现类：`ProductFootprintServiceImpl`

足迹 ZSet 的数据模型为：

```text
member = productId
score  = 浏览时间戳（毫秒）
```

选择 ZSet 的原因：

1. 同一商品作为相同 member，只保留一条记录；再次浏览会更新 score。
2. 按 score 倒序查询即可得到最近浏览商品。
3. 可以按 rank 删除最旧记录，把每个用户的足迹限制在最近 100 条。
4. 同时满足“去重”和“按时间排序”，比 List 更贴合业务模型。

足迹 Key 保存 90 天。Redis 只保存商品 ID 和时间，展示时再批量查询 MySQL 获取当前有效商品及最新价格，避免把容易变化的商品详情长期固化在足迹缓存中。

### 5. 商品热度系统：String + ZSet

实现类：`ProductHotServiceImpl`

商品热度链路使用了多种 Redis 能力：

#### (1) String 做短期去重和消息幂等

- 浏览去重 Key 使用 `setIfAbsent`，同一用户或设备 5 分钟内重复浏览同一商品只计一次。
- 热度事件处理 Key 使用 `setIfAbsent`，同一个事件 ID 在 48 小时内只累计一次分数。

这类数据本质上只关心“是否存在”，String + `SET NX` 的模型最简单，且能直接附带 TTL。

#### (2) ZSet 做小时热度桶

每个小时使用一个 ZSet：

```text
mall:product:hot:hour:{yyyyMMddHH}
member = productId
score  = 当小时累计热度分
```

消费浏览等热度事件时，通过 `addScore` 累加商品分值。ZSet 既能原子累加分值，又能按分值排序，很适合排行榜。

#### (3) ZSet 做首页加权榜单

定时任务读取最近 24 个小时桶，越旧的小时权重越低，使用 Redis 服务端的 ZSet 加权并集完成聚合。聚合结果先写入临时榜单，裁剪到最多 50 条后再重命名为正式榜单，避免首页读取到半成品。

首页读取时按 score 倒序取热门商品；Redis 为空或异常时，代码会回退到 MySQL 推荐排序，具备基本降级能力。

### 6. 用户签到：Bitmap

实现类：`RedisSignInService`

每个用户每个月使用一个 Bitmap，日期映射为位偏移：

```text
Key    = mall:user:sign:{userId}:{yyyyMM}
offset = dayOfMonth - 1
value  = 1 表示已签到
```

选择 Bitmap 的原因：

1. 一个月最多 31 天，只需要 31 个 bit，空间占用极小。
2. 判断某天是否签到只需要读取一个 bit。
3. 把同一个 bit 重复写为 1 天然幂等。
4. `cardinality` 可以统计当月签到天数。
5. 顺序检查 bit 可以计算连续签到天数并生成月度、年度签到明细。

签到 Bitmap 保存 400 天。目前签到事实数据只保存在 Redis 中，Redis 数据丢失会直接影响历史签到查询和连续签到计算，因此它在这里不只是缓存，而是阶段性的业务数据存储。

### 7. 购物车：Hash + String + Lua

实现类：`CartCacheService`

每个用户使用一个 Hash：

```text
Key   = mall:cart:user:{userId}
Field = skuId
Value = {"quantity":数量,"version":版本号,"deleted":是否为删除墓碑}
```

选择 Hash 的原因：

1. 一个用户的购物车天然是一组 SKU 项，适合聚合在一个 Key 下。
2. 修改单个 SKU 时只需要更新对应 Field，不需要重写整个购物车。
3. 查询购物车时可一次读取整个 Hash。
4. 按用户设置统一 TTL，便于管理缓存生命周期。

购物车保留**版本号和删除墓碑**，主要服务于 Redis、RabbitMQ、MySQL 之间的异步一致性：

- 版本号保证 MySQL 只应用更新版本的数据。
- 删除墓碑防止缓存重建或乱序消息把已删除商品恢复出来。
- 消息发送失败时，Lua 只在当前版本仍等于失败版本的情况下回滚，避免覆盖后续成功操作。

购物车同时使用 `mall:cart:loaded:{userId}` String 标记，区分“空购物车”和“尚未从 MySQL 重建缓存”。

购物车的增加、修改和条件回滚均通过 Lua 执行，以保证下列步骤不可被并发请求插入：

1. 读取旧值。
2. 计算数量并校验单 SKU 最大数量 99。
3. 增加单调递增版本号。
4. 写入 Hash。
5. 刷新购物车和 loaded 标记 TTL。

### 8. 秒杀库存、限购与幂等：String 计数器 + Lua

实现类：`SeckillRedisService`

一次秒杀请求会同时操作三个 String Key：

1. `stock`：剩余库存计数器。
2. `user`：用户已预扣数量计数器。
3. `request`：请求 ID 幂等标记。

选择 String 的原因：

1. 库存和限购值都是单个整数，String 计数器最直接。
2. Redis 的 `INCRBY`、`DECRBY` 适合高并发计数。
3. 请求幂等只关心 Key 是否存在，无需复杂结构。
4. 各 Key 可以独立过期，活动结束后自动释放。

Lua 脚本在一次原子执行中完成：

1. 检查库存是否已经预热。
2. 检查同一个请求 ID 是否重复。
3. 检查用户累计购买数是否超过限购。
4. 检查剩余库存是否充足。
5. 扣减库存、增加用户占用、写入幂等标记。
6. 设置活动结束时间再加 5 分钟的 TTL。

订单创建失败或超时取消时，补偿脚本会恢复库存、回退用户占用并删除请求幂等标记。Lua 在这里的核心价值是防止并发下出现超卖、限购失效和部分更新。

预热脚本只在库存 Key 不存在时写入初始值；已经预热的库存不会被定时任务覆盖，只刷新 TTL，从而避免活动进行中把已扣减库存重置为数据库初始库存。

### 9. 秒杀防刷：ZSet + Lua

实现类：`SeckillRateLimitService`

项目分别按用户和 IP 建立滑动窗口，默认配置为：

- 5 秒内单用户最多 3 次。
- 5 秒内单 IP 最多 10 次。

ZSet 的数据模型为：

```text
member = 当前时间戳 + UUID
score  = 当前时间戳（毫秒）
```

选择 ZSet 的原因：

1. score 可以表示请求发生时间。
2. 可以按 score 删除窗口外的旧请求。
3. `ZCARD` 可以统计当前窗口内请求数。
4. 随机 member 保证同一毫秒内的多个请求不会相互覆盖。

Lua 将“删除过期记录、统计数量、判断限额、写入本次请求、设置 TTL”合并为原子操作。与简单固定窗口 `INCR` 相比，ZSet 滑动窗口边界更平滑、限流更准确，但每次请求都保存一条成员记录，内存成本更高。

### 10. 定时任务互斥：RLock

实现类：`RedisDistributedLockAspect`

以下五个定时任务使用统一的 `@RedisDistributedLock` 注解：

| 任务 | Lock Key | 作用 |
| --- | --- | --- |
| 商品热度聚合 | `mall:product:hot-aggregation` | 防止多个实例重复计算并替换热门榜单 |
| Outbox 投递 | `mall:outbox:relay` | 防止多个实例重复扫描和投递待发送消息 |
| 订单超时补偿 | `mall:order:timeout-compensation` | 防止重复扫描并取消超时订单 |
| 秒杀库存预热 | `mall:seckill:preheat` | 防止多个实例同时预热活动库存 |
| 秒杀超时补偿 | `mall:seckill:compensation` | 防止多个实例重复补偿秒杀请求 |

切面使用非阻塞 `tryLock()`：抢不到锁的实例直接跳过本轮任务，抢到锁的实例执行结束后释放锁。这里追求的是“同一轮最多一个实例执行”，而不是让所有实例排队执行。

## 五、Lua 脚本解决的并发问题

项目中的 Lua 不是一种数据结构，而是保证多条 Redis 命令原子性的关键手段。

| 场景 | 如果拆成多条 Java/Redis 命令 | Lua 带来的保证 |
| --- | --- | --- |
| 购物车累加 | 两个请求可能读取相同旧数量，产生丢失更新；数量上限可能被并发穿透 | 读取、计算、校验、版本递增和写入原子执行 |
| 购物车回滚 | 失败请求可能覆盖后续已成功的新版本 | 仅当当前版本等于失败版本时回滚 |
| 秒杀预扣 | 库存、限购、幂等标记可能只更新一部分，或者并发超卖 | 三项校验与三项写入原子执行 |
| 秒杀补偿 | 重复补偿可能多加库存 | 只有请求幂等标记存在时才补偿并删除标记 |
| 滑动窗口限流 | 多个请求可能同时通过数量判断 | 清理、计数、判断和新增请求原子执行 |

## 六、缓存一致性与失效策略

### 1. TTL 设计

项目大量使用 TTL，主要目的包括：

1. 自动清理登录态、幂等标记、限流记录和活动状态。
2. 避免收藏、购物车、详情缓存永久保存旧数据。
3. 通过随机 TTL 分散商品详情和收藏缓存的过期时间，降低缓存雪崩概率。
4. 为热度小时桶、足迹和签到保留符合业务窗口的历史数据。

### 2. 数据库与 Redis 的角色

| 场景 | 权威数据源 | Redis 的角色 |
| --- | --- | --- |
| 商品详情 | MySQL | Cache Aside 查询缓存 |
| 商品收藏 | MySQL | 集合查询加速，可从 MySQL 重建 |
| 浏览足迹 | Redis | 直接保存最近浏览事实，商品展示信息回查 MySQL |
| 商品热度 | Redis | 短期事件聚合和榜单存储 |
| 用户签到 | Redis | 当前实现中的签到事实存储 |
| 购物车 | Redis + MySQL | Redis 保存实时状态，RabbitMQ 异步持久化到 MySQL |
| 秒杀预扣 | Redis + MySQL 业务流水/订单 | Redis 承担高并发前置校验与库存预扣 |
| 登录会话 | Redis | 会话状态存储 |

## 七、生产环境需要重点关注的问题

### 1. Redis Cluster 的多 Key Lua 兼容性

购物车脚本一次操作 `mall:cart:user:{userId}` 和 `mall:cart:loaded:{userId}`；秒杀脚本一次操作 stock、user、request 三个 Key。当前 Key 没有使用 Redis Cluster Hash Tag。

如果以后从单节点迁移到 Redis Cluster，多 Key Lua 要求所有 Key 位于同一个 slot，否则可能出现 `CROSSSLOT`。建议迁移前统一改为带相同 Hash Tag 的 Key，例如：

```text
mall:cart:{userId}:items
mall:cart:{userId}:loaded

mall:seckill:{seckillSkuId}:stock
mall:seckill:{seckillSkuId}:user:{userId}
mall:seckill:{seckillSkuId}:request:{userId}:{requestId}
```

### 2. 签到和足迹的数据持久性

签到和足迹目前没有独立的 MySQL 明细作为恢复来源。需要明确 Redis 的持久化、备份和高可用策略；如果业务未来要求永久审计、运营统计或灾难恢复，应补充数据库明细或异步归档。

### 3. 商品热度事件的处理顺序

当前热度事件先写入“已处理”幂等 Key，再给小时 ZSet 加分。如果幂等 Key 写入成功后，ZSet 更新失败，该事件在 48 小时内重试时会被视为已处理，可能丢失一次热度分值。生产环境可考虑使用 Lua 把“幂等判断和 ZSet 加分”合并为原子操作。

### 4. 非原子组合操作

足迹的“写入、统计长度、裁剪、刷新 TTL”和收藏/购物车的部分重建流程由多条命令组成，并非整体原子。高并发下可能短暂超过数量上限，或与同时发生的写操作产生竞态。若并发量提高，可改为 Lua、事务或按用户加锁。

### 5. 热门详情锁的租约

热门商品详情锁显式设置了 10 秒租约。如果数据库查询和缓存序列化超过租约，锁可能在业务完成前释放。应结合慢查询和 P99 回源耗时调整租约，释放时也应关注锁是否仍由当前线程持有。

### 6. 单节点可用性

当前配置只展示了单节点 Redis。由于登录、签到、购物车和秒杀都依赖 Redis，Redis 故障不仅会造成缓存未命中，还可能导致核心功能不可用或业务状态丢失。生产部署应补充高可用、持久化、备份、内存淘汰策略、慢命令和连接池监控。

## 八、核心源码索引

### 1. 配置与依赖

- `mall-backend/pom.xml`
- `mall-backend/mall-app/src/main/resources/application.yml`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/config/ProductCacheProperties.java`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/config/ProductHotProperties.java`

### 2. 用户模块

- `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/service/impl/RedisLoginSessionService.java`
- `mall-backend/mall-user/src/main/java/com/tuzki/mall/user/service/impl/RedisSignInService.java`

### 3. 商品模块

- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/ProductDetailCacheService.java`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/ProductHotDetailCacheService.java`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/ProductFavoriteCacheService.java`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/impl/ProductFootprintServiceImpl.java`
- `mall-backend/mall-product/src/main/java/com/tuzki/mall/product/service/impl/ProductHotServiceImpl.java`
- `mall-backend/mall-admin/src/main/java/com/tuzki/mall/admin/product/service/ProductCacheInvalidationService.java`

### 4. 购物车与秒杀模块

- `mall-backend/mall-cart/src/main/java/com/tuzki/mall/cart/service/CartCacheService.java`
- `mall-backend/mall-seckill/src/main/java/com/tuzki/mall/seckill/redis/SeckillRedisService.java`
- `mall-backend/mall-app/src/main/java/com/tuzki/mall/seckill/ratelimit/SeckillRateLimitService.java`

### 5. 分布式锁

- `mall-backend/mall-app/src/main/java/com/tuzki/mall/scheduling/lock/RedisDistributedLock.java`
- `mall-backend/mall-app/src/main/java/com/tuzki/mall/scheduling/lock/RedisDistributedLockAspect.java`

## 九、一句话总结

本项目根据业务访问模式选择 Redis 数据结构：**单值和计数用 String、按实体字段局部更新用 Hash、成员关系用 Set、需要排序和时间窗口用 ZSet、布尔日期序列用 Bitmap**；再用 **Lua 保证多步操作原子性，用 RLock 完成跨实例互斥**。整体选型与业务模型匹配度较高，后续生产化的重点是 Redis 高可用、历史数据持久化，以及 Redis Cluster 下多 Key Lua 的 slot 规划。
