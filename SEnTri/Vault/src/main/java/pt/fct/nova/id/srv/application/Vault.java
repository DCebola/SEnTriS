package pt.fct.nova.id.srv.application;

import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Vault {

    public static void saveSecrets(String storeID, Map<byte[], byte[]> secrets) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(storeID.getBytes(StandardCharsets.UTF_8), secrets);
        }
    }

    public static Map<byte[],byte[]> getSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(storeID.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void deleteSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(storeID.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static boolean exists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID.getBytes(StandardCharsets.UTF_8));
        }
    }
}
