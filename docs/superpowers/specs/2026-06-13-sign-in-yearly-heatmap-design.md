# 用户年度签到热力图设计

## 一、目标

年度详情页用于展示用户某一年 12 个月的签到热力图。首版采用“12 个月矩阵总览”布局，一屏展示年度签到总天数和每月签到明细。

当前功能只做签到年度详情，不扩展补签、排行榜、积分、优惠券列表和订单用券。

## 二、是否落 MySQL

首版不新增 MySQL 签到明细表。年度热力图直接读取现有 Redis Bitmap：

```text
mall:user:sign:{userId}:{yyyyMM}
```

原因：

1. 签到事实数据已经按用户和月份保存在 Redis Bitmap。
2. 年度页是读展示场景，不涉及交易强一致。
3. 单用户一年只需要读取 12 个 Bitmap，每个月最多遍历 31 天，成本很低。
4. MySQL 继续保存优惠券模板和用户优惠券到账记录，这些数据需要持久化、幂等和审计。

只有出现永久审计、全站统计、运营 BI、Redis 历史恢复等需求时，再增加 MySQL 签到明细表或年度汇总表。

## 三、接口设计

新增接口：

```text
GET /api/users/sign-in/yearly?year=2026
```

规则：

1. 依赖登录态解析当前用户，不允许前端传 `userId`。
2. `year` 不传时使用业务时区 `Asia/Shanghai` 的当前年份。
3. `year` 合法范围为 `2000` 到 `当前年份 + 1`。
4. 查询失败不产生发券副作用，只读取签到状态。

响应结构：

```json
{
  "year": 2026,
  "yearSignedCount": 184,
  "months": [
    {
      "month": 1,
      "daysInMonth": 31,
      "signedCount": 18,
      "signedDays": [1, 2, 3, 5]
    }
  ]
}
```

## 四、后端设计

新增视图对象：

1. `SignInYearlyProfileVO`：年度签到总览。
2. `SignInMonthProfileVO`：单月签到数据。

`SignInService` 新增：

```java
SignInYearlyProfileVO getYearlyProfile(Long userId, Integer year);
```

`RedisSignInService` 实现逻辑：

1. 解析目标年份。
2. 循环 1 到 12 月构造 `YearMonth`。
3. 按月份读取 Redis Bitmap。
4. 遍历该月天数生成 `signedDays`。
5. 统计 `signedCount` 和 `yearSignedCount`。

## 五、前端设计

新增页面：

```text
/profile/sign-in/yearly
```

页面结构：

1. 顶部展示年份、年度签到总天数。
2. 下方按 3 列或响应式 2 列展示 12 个月。
3. 每个月展示月份、签到天数和每日小方块。
4. 个人主页本月签到模块增加“年度详情”入口。

## 六、测试设计

重点覆盖：

1. 年度接口可以返回 12 个月数据。
2. 闰年 2 月为 29 天，平年 2 月为 28 天。
3. 未签到月份返回空 `signedDays` 和 `signedCount = 0`。
4. 缺省年份使用当前业务年份。
5. 未登录访问年度接口返回 401。
