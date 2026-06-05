package com.tuzki.mall.seckill.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 秒杀 Redis 服务，负责活动库存预热、Lua 原子预扣和失败补偿。
 */
@Service
public class SeckillRedisService {

    public static final long PRE_DEDUCT_SUCCESS = 0L;

    public static final long DUPLICATED_REQUEST = 1L;

    public static final long PURCHASE_LIMIT_EXCEEDED = 2L;

    public static final long STOCK_SOLD_OUT = 3L;

    public static final long STOCK_NOT_PREHEATED = 4L;

    private static final String PRE_DEDUCT_SCRIPT = """
            local stock = redis.call('GET', KEYS[1])
            if not stock then return 4 end
            if redis.call('EXISTS', KEYS[3]) == 1 then return 1 end
            local current = redis.call('GET', KEYS[2])
            if current and tonumber(current) + tonumber(ARGV[1]) > tonumber(ARGV[2]) then return 2 end
            if tonumber(stock) < tonumber(ARGV[1]) then return 3 end
            redis.call('DECRBY', KEYS[1], ARGV[1])
            redis.call('INCRBY', KEYS[2], ARGV[1])
            redis.call('SET', KEYS[3], '1')
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('EXPIRE', KEYS[2], ARGV[3])
            redis.call('EXPIRE', KEYS[3], ARGV[3])
            return 0
            """;

    private static final String COMPENSATE_SCRIPT = """
            redis.call('INCRBY', KEYS[1], ARGV[1])
            local current = redis.call('DECRBY', KEYS[2], ARGV[1])
            if current <= 0 then
                redis.call('DEL', KEYS[2])
            end
            redis.call('DEL', KEYS[3])
            return 1
            """;

    private final RedissonClient redissonClient;

    public SeckillRedisService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 将活动商品库存预热到 Redis，并设置到活动结束后的过期时间。
     *
     * @param seckillSkuId 秒杀活动商品 ID
     * @param stockCount 活动库存数量
     * @param ttl Redis key 存活时间
     */
    public void preheatStock(Long seckillSkuId, Integer stockCount, Duration ttl) {
        RBucket<String> bucket = redissonClient.getBucket(stockKey(seckillSkuId), StringCodec.INSTANCE);
        bucket.set(String.valueOf(stockCount), normalizeTtl(ttl));
    }

    /**
     * 原子校验秒杀库存、用户限购和请求幂等，并在成功时扣减活动库存。
     *
     * @param seckillSkuId 秒杀活动商品 ID
     * @param userId 用户 ID
     * @param requestId 秒杀请求幂等号
     * @param quantity 本次购买数量
     * @param limitQuantity 每人限购数量
     * @param ttl Redis key 存活时间
     * @return Lua 返回码，见本类常量定义
     */
    public long preDeduct(Long seckillSkuId,
                          Long userId,
                          String requestId,
                          Integer quantity,
                          Integer limitQuantity,
                          Duration ttl) {
        Long result = script().eval(
                RScript.Mode.READ_WRITE,
                PRE_DEDUCT_SCRIPT,
                RScript.ReturnType.LONG,
                List.of(stockKey(seckillSkuId), userKey(seckillSkuId, userId), requestKey(seckillSkuId, userId, requestId)),
                String.valueOf(quantity),
                String.valueOf(limitQuantity),
                String.valueOf(normalizeTtl(ttl).toSeconds())
        );
        return result == null ? STOCK_SOLD_OUT : result;
    }

    /**
     * 订单创建失败时补偿 Redis 活动库存、用户限购占用和请求幂等标记。
     *
     * @param seckillSkuId 秒杀活动商品 ID
     * @param userId 用户 ID
     * @param requestId 秒杀请求幂等号
     * @param quantity 需要补偿的购买数量
     */
    public void compensate(Long seckillSkuId, Long userId, String requestId, Integer quantity) {
        script().eval(
                RScript.Mode.READ_WRITE,
                COMPENSATE_SCRIPT,
                RScript.ReturnType.LONG,
                List.of(stockKey(seckillSkuId), userKey(seckillSkuId, userId), requestKey(seckillSkuId, userId, requestId)),
                String.valueOf(quantity)
        );
    }

    public String stockKey(Long seckillSkuId) {
        return "mall:seckill:stock:" + seckillSkuId;
    }

    public String userKey(Long seckillSkuId, Long userId) {
        return "mall:seckill:user:" + seckillSkuId + ":" + userId;
    }

    public String requestKey(Long seckillSkuId, Long userId, String requestId) {
        return "mall:seckill:request:" + seckillSkuId + ":" + userId + ":" + requestId;
    }

    private Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofSeconds(1);
        }
        return ttl;
    }

    private RScript script() {
        return redissonClient.getScript(StringCodec.INSTANCE);
    }
}
