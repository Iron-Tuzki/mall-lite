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
