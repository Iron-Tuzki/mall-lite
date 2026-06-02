package com.tuzki.mall.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuzki.mall.cart.entity.CartItem;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车 Redis 缓存服务，使用 Lua 脚本原子维护数量、墓碑和单调递增版本号。
 */
@Service
public class CartCacheService {

    private static final String CART_KEY_PREFIX = "mall:cart:user:";

    private static final String CART_LOADED_KEY_PREFIX = "mall:cart:loaded:";

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private static final String MUTATE_SCRIPT = """
            local old = redis.call('HGET', KEYS[1], ARGV[1])
            local oldValue = old and cjson.decode(old) or { quantity = 0, version = 0, deleted = true }
            local newValue = cjson.encode({
              quantity = tonumber(ARGV[2]),
              version = tonumber(oldValue.version) + 1,
              deleted = ARGV[3] == 'true'
            })
            redis.call('HSET', KEYS[1], ARGV[1], newValue)
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])
            return { old or '', newValue }
            """;

    // sugus:购物车的取值、计算、判上限、更新数据、设置过期时间等核心逻辑
    /**
     * Lua 语法基础：模拟三元运算符
     * Lua 没有原生三元表达式，业界通用 A and B or C 写法，等价于：
     * 如果 A 为真 → 取 B；如果 A 为假 → 取 C
     */
    private static final String ADD_SCRIPT = """
            local old = redis.call('HGET', KEYS[1], ARGV[1])
            local oldValue = old and cjson.decode(old) or { quantity = 0, version = 0, deleted = true }
            local oldQuantity = oldValue.deleted and 0 or tonumber(oldValue.quantity)
            local newQuantity = oldQuantity + tonumber(ARGV[2])
            if newQuantity > 99 then
              return { 'LIMIT', old or '' }
            end
            local newValue = cjson.encode({
              quantity = newQuantity,
              version = tonumber(oldValue.version) + 1,
              deleted = false
            })
            redis.call('HSET', KEYS[1], ARGV[1], newValue)
            redis.call('EXPIRE', KEYS[1], ARGV[3])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
            return { old or '', newValue }
            """;

    /**
     * 回滚redis
     * 如果当前用户购物车没有这个商品，直接返回
     * 如果最新版本号不等于失败的版本号，直接返回
     * 如果旧数据为空，则删除这个field（skuid）
     * 旧数据不为空，则设置这恶field为旧数据
     */
    private static final String ROLLBACK_SCRIPT = """
            local current = redis.call('HGET', KEYS[1], ARGV[1])
            if not current then
              return 0
            end
            local currentValue = cjson.decode(current)
            if tonumber(currentValue.version) ~= tonumber(ARGV[2]) then
              return 0
            end
            if ARGV[3] == '' then
              redis.call('HDEL', KEYS[1], ARGV[1])
            else
              redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])
            end
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[4])
            return 1
            """;

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    public CartCacheService(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 判断用户购物车是否已经从 MySQL 完整加载。
     *
     * @param userId 用户 ID
     * @return loaded 标记存在时返回 true
     */
    public boolean isLoaded(Long userId) {
        return loadedBucket(userId).isExists();
    }

    /**
     * 原子更新购物车项，并返回变更前后状态。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @param quantity 最新数量
     * @param deleted 是否写入删除墓碑
     * @return 缓存变更结果
     */
    public CartCacheMutation mutate(Long userId, Long skuId, Integer quantity, boolean deleted) {
        List<Object> result = script().eval(
                RScript.Mode.READ_WRITE,
                MUTATE_SCRIPT,
                RScript.ReturnType.LIST,
                List.of(cartKey(userId), loadedKey(userId)),
                String.valueOf(skuId),
                String.valueOf(quantity),
                String.valueOf(deleted),
                String.valueOf(CACHE_TTL.toSeconds())
        );
        String previousJson = toNullableString(result.get(0));
        return new CartCacheMutation(previousJson, readValue(String.valueOf(result.get(1))));
    }

    /**
     * 原子累加购物车项数量，并在 Lua 脚本内校验单个 SKU 的数量上限。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @param quantity 增加数量
     * @return 缓存变更结果
     */
    public CartCacheMutation add(Long userId, Long skuId, Integer quantity) {
        List<Object> result = script().eval(
                RScript.Mode.READ_WRITE,
                ADD_SCRIPT,
                RScript.ReturnType.LIST,
                List.of(cartKey(userId), loadedKey(userId)),    // Redis Lua 规范：所有 Redis Key 必须集中放在这一个参数里（KEYS 数组）
                String.valueOf(skuId),  // Lua 脚本的自定义参数（ARGV 数组）
                String.valueOf(quantity),   // Lua 脚本的自定义参数（ARGV 数组）
                String.valueOf(CACHE_TTL.toSeconds())   // Lua 脚本的自定义参数（ARGV 数组）
        );
        if ("LIMIT".equals(String.valueOf(result.get(0)))) {
            throw new IllegalArgumentException("cart item quantity must not be greater than 99");
        }
        return new CartCacheMutation(toNullableString(result.get(0)), readValue(String.valueOf(result.get(1))));
    }

    /**
     * 仅在当前版本仍等于失败请求写入版本时恢复旧值，避免覆盖后续成功请求。
     *
     * @param userId 用户 ID
     * @param skuId SKU ID
     * @param failedVersion 发送失败请求写入的版本号
     * @param previousJson 变更前 JSON，新建字段时为 null
     * @return 成功回滚返回 true，检测到更高版本返回 false
     */
    public boolean rollback(Long userId, Long skuId, Long failedVersion, String previousJson) {
        Long result = script().eval(
                RScript.Mode.READ_WRITE,
                ROLLBACK_SCRIPT,
                RScript.ReturnType.LONG,
                List.of(cartKey(userId), loadedKey(userId)),
                String.valueOf(skuId),
                String.valueOf(failedVersion),
                previousJson == null ? "" : previousJson,
                String.valueOf(CACHE_TTL.toSeconds())
        );
        return result != null && result == 1L;
    }

    /**
     * 从 MySQL 全量记录重建用户购物车缓存，包含逻辑删除墓碑。
     *
     * @param userId 用户 ID
     * @param items MySQL 中该用户全部购物车项
     */
    public void rebuild(Long userId, Collection<CartItem> items) {
        RMap<String, String> cartMap = cartMap(userId);
        cartMap.delete();
        if (items != null && !items.isEmpty()) {
            Map<String, String> values = new LinkedHashMap<>();
            for (CartItem item : items) {
                values.put(String.valueOf(item.getSkuId()), writeValue(new CartCacheItem(
                        item.getQuantity(),
                        item.getVersion(),
                        item.getDeleted() != null && item.getDeleted() == 1
                )));
            }
            cartMap.putAll(values);
            cartMap.expire(CACHE_TTL);
        }
        loadedBucket(userId).set("1", CACHE_TTL);
    }

    /**
     * 查询用户购物车缓存中的全部值，包含删除墓碑。
     *
     * @param userId 用户 ID
     * @return SKU ID 与缓存项映射
     */
    public Map<Long, CartCacheItem> getAll(Long userId) {
        Map<Long, CartCacheItem> result = new LinkedHashMap<>();
        cartMap(userId).readAllMap().forEach((skuId, json) ->
                result.put(Long.valueOf(skuId), readValue(json)));
        return result;
    }

    private RScript script() {
        return redissonClient.getScript(StringCodec.INSTANCE);
    }

    private RMap<String, String> cartMap(Long userId) {
        return redissonClient.getMap(cartKey(userId), StringCodec.INSTANCE);
    }

    private RBucket<String> loadedBucket(Long userId) {
        return redissonClient.getBucket(loadedKey(userId), StringCodec.INSTANCE);
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String loadedKey(Long userId) {
        return CART_LOADED_KEY_PREFIX + userId;
    }

    private CartCacheItem readValue(String json) {
        try {
            return objectMapper.readValue(json, CartCacheItem.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("read cart cache item failed", exception);
        }
    }

    private String writeValue(CartCacheItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("write cart cache item failed", exception);
        }
    }

    private String toNullableString(Object value) {
        String text = String.valueOf(value);
        return text.isEmpty() ? null : text;
    }
}
