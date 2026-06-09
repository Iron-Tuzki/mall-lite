# CountDownLatch 并发秒杀测试笔记

## 一、背景

本文记录 `SeckillManualConcurrentIntegrationTest#concurrentSeckillRequestsWithCommittedTransactionsForManualReview` 中，使用 `CountDownLatch` 模拟并发秒杀请求的核心思路。

这个测试的目标不是简单地启动多个线程，而是尽量让多个请求线程在同一个“起跑点”附近同时发起请求，从而更容易验证库存扣减、事务提交、并发竞争和失败结果是否符合预期。

## 二、测试方法代码

```java
@Test
void concurrentSeckillRequestsWithCommittedTransactionsForManualReview() throws Exception {
    Long skuId = 900017L;
    Long[] userIds = {999999L, 1000000L, 900313L};
    Long[] addressIds = {900059L, 900118L, 900119L};
    int expectedSuccessCount = 2;
    int expectedFailCount = 1;

    Long seckillSkuId = resolveActiveSeckillSkuId(skuId);
    int concurrentRequests = userIds.length;
    assertEquals(concurrentRequests, addressIds.length);

    ExecutorService executorService = Executors.newFixedThreadPool(concurrentRequests);
    try {
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentRequests; i++) {
            int requestIndex = i;
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                assertTrue(startLatch.await(5, TimeUnit.SECONDS)); // 所有请求线程阻塞在这里
                String token = loginSessionService.createSession(userIds[requestIndex]);
                String requestId = "mc-" + requestIndex + "-" + UUID.randomUUID().toString().substring(0, 8);
                return mockMvc.perform(post("/api/seckill/orders")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(seckillOrderRequest(seckillSkuId, requestId, addressIds[requestIndex], 1)))
                        .andExpect(status().isOk())
                        .andReturn();
            }));
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));// 主线程等待所有子线程准备好。CountDownLatch 的特点是：计数一旦归零，就永久打开。
        startLatch.countDown(); // 主线程通知所有请求线程开始执行

        int successCount = 0;
        int failCount = 0;
        for (Future<MvcResult> future : futures) {
            MvcResult result = future.get(10, TimeUnit.SECONDS);
            String responseBody = result.getResponse().getContentAsString();
            System.out.println(responseBody);
            JsonNode responseJson = objectMapper.readTree(responseBody);
            if (responseJson.path("success").asBoolean(false)) {
                successCount++;
            } else {
                failCount++;
            }
        }
        assertEquals(expectedSuccessCount, successCount);
        assertEquals(expectedFailCount, failCount);
    } finally {
        executorService.shutdownNow();
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
    }
}
```

## 三、两个 CountDownLatch 的职责

### 1. readyLatch：确认所有子线程已经准备好

```java
CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
```

`readyLatch` 的初始计数等于并发请求数。每个子线程启动后先执行：

```java
readyLatch.countDown();
```

含义是：当前请求线程已经启动，并且即将进入等待统一放行的位置。

主线程执行：

```java
assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
```

含义是：最多等待 5 秒，直到所有请求线程都执行过 `readyLatch.countDown()`。

### 2. startLatch：统一释放所有请求线程

```java
CountDownLatch startLatch = new CountDownLatch(1);
```

每个子线程执行完 `readyLatch.countDown()` 后，会执行：

```java
assertTrue(startLatch.await(5, TimeUnit.SECONDS));
```

只要主线程还没有执行 `startLatch.countDown()`，请求线程就会阻塞在这里，不会继续执行 `mockMvc.perform(...)`。

主线程确认所有子线程准备好后，执行：

```java
startLatch.countDown();
```

这相当于统一打开闸门，让请求线程开始继续执行秒杀请求。

## 四、关键时序理解

### 1. 常见执行顺序

```text
主线程 submit 线程 1
线程 1 readyLatch.countDown()
线程 1 startLatch.await() 阻塞

主线程 submit 线程 2
线程 2 readyLatch.countDown()
线程 2 startLatch.await() 阻塞

主线程 submit 线程 3
线程 3 readyLatch.countDown()
线程 3 startLatch.await() 阻塞

主线程 readyLatch.await(...) 返回 true
主线程 startLatch.countDown()

线程 1、线程 2、线程 3 继续执行 mockMvc.perform(...)
```

### 2. 可能出现：子线程都 countDown 完了，主线程才 await

完全可能出现这种情况：

1. 主线程还在 `for` 循环里提交任务。
2. 线程池中的 3 个子线程已经分别执行了 `readyLatch.countDown()`。
3. `readyLatch` 的计数已经变成 0。
4. 主线程结束 `for` 循环后，才执行 `readyLatch.await(5, TimeUnit.SECONDS)`。

这没有问题，因为 `CountDownLatch` 的特点是：**计数一旦归零，就永久打开**。

所以主线程后续再调用：

```java
readyLatch.await(5, TimeUnit.SECONDS)
```

会立即返回 `true`，不会等待 5 秒，也不会丢失之前的 `countDown()` 信号。

### 3. 子线程不会提前执行请求

即使 3 个子线程在主线程调用 `readyLatch.await(...)` 之前，就已经全部执行完 `readyLatch.countDown()`，它们也不会提前发请求。

原因是子线程后面还有：

```java
startLatch.await(5, TimeUnit.SECONDS)
```

在主线程执行 `startLatch.countDown()` 之前，请求线程会被挡在这里。

## 五、关于第三个线程

### 1. 第三个线程把 readyLatch 减到 0 后会怎样

假设 `readyLatch` 初始值是 3：

1. 线程 1 执行 `readyLatch.countDown()`，计数从 3 变成 2。
2. 线程 2 执行 `readyLatch.countDown()`，计数从 2 变成 1。
3. 线程 3 执行 `readyLatch.countDown()`，计数从 1 变成 0。
4. 主线程从 `readyLatch.await(...)` 中被唤醒。
5. 主线程执行 `startLatch.countDown()`。
6. 子线程通过 `startLatch.await(...)`，继续执行请求。

### 2. 第三个线程还没 await，主线程就 countDown 了，也没问题

可能出现这个时序：

```text
线程 3 执行 readyLatch.countDown()
readyLatch 归零
主线程 readyLatch.await(...) 被唤醒
主线程执行 startLatch.countDown()
线程 3 才执行 startLatch.await(...)
```

这也没问题。

因为 `startLatch` 一旦被主线程 `countDown()` 到 0，后续任何线程再调用：

```java
startLatch.await()
```

都会立即通过，不会再阻塞。

## 六、为什么说“几乎同时被释放”

这里的“几乎同时”不是指 CPU 层面绝对同一纳秒执行，而是指测试代码尽量做到：

1. 所有请求线程都已经被线程池调度启动。
2. 请求线程都已经到达或非常接近 `startLatch.await(...)` 这个起跑点。
3. 主线程统一执行 `startLatch.countDown()` 打开闸门。
4. 多个请求线程在很短时间窗口内继续执行 `mockMvc.perform(...)`。

因此，这种写法可以尽量缩短多个秒杀请求之间的启动时间差，更容易模拟真实并发竞争。

## 七、为什么 await 要设置超时时间

代码中使用：

```java
readyLatch.await(5, TimeUnit.SECONDS)
startLatch.await(5, TimeUnit.SECONDS)
future.get(10, TimeUnit.SECONDS)
```

核心目的是避免测试无限卡死。

如果某个子线程没有正常启动，或者线程池调度出现异常，那么 `readyLatch` 可能永远不会归零。如果使用无超时的 `await()`，测试会一直挂住。

加上超时后：

1. 正常情况下，等待条件满足，返回 `true`。
2. 异常情况下，超时返回 `false`。
3. `assertTrue(...)` 会让测试快速失败。
4. 失败原因更容易定位。

## 八、总结

这个测试使用的是典型的“双 CountDownLatch 并发起跑”模型：

| 组件 | 初始计数 | 谁调用 countDown | 谁调用 await | 作用 |
|---|---:|---|---|---|
| `readyLatch` | 请求线程数 | 子线程 | 主线程 | 确认所有子线程已准备好 |
| `startLatch` | 1 | 主线程 | 子线程 | 统一放行所有请求线程 |

记忆方式：

1. `readyLatch`：子线程告诉主线程“我准备好了”。
2. `startLatch`：主线程告诉子线程“可以开始了”。
3. `CountDownLatch` 计数归零后会永久打开，不会丢失已经发生的 `countDown()`。
4. “几乎同时释放”是工程意义上的同时，用来尽量提高并发请求的重叠程度，而不是保证线程在 CPU 层面绝对同时执行。
