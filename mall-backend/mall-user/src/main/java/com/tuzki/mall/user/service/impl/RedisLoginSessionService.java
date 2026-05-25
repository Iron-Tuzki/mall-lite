package com.tuzki.mall.user.service.impl;

import com.tuzki.mall.user.service.LoginSessionService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * 基于 Redisson 的登录态服务实现，使用随机 token 作为客户端登录凭证。
 */
@Service
public class RedisLoginSessionService implements LoginSessionService {

    private static final String LOGIN_TOKEN_KEY_PREFIX = "mall:user:login:";

    private static final Duration SESSION_TTL = Duration.ofDays(7);

    private final RedissonClient redissonClient;

    public RedisLoginSessionService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public String createSession(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        RBucket<String> sessionBucket = redissonClient.getBucket(buildKey(token));
        sessionBucket.set(String.valueOf(userId), SESSION_TTL);
        return token;
    }

    @Override
    public Long getUserId(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        RBucket<String> sessionBucket = redissonClient.getBucket(buildKey(token));
        String userId = sessionBucket.get();
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return Long.valueOf(userId);
    }

    @Override
    public void deleteSession(String token) {
        if (StringUtils.hasText(token)) {
            redissonClient.getBucket(buildKey(token)).delete();
        }
    }

    private String buildKey(String token) {
        return LOGIN_TOKEN_KEY_PREFIX + token;
    }
}
