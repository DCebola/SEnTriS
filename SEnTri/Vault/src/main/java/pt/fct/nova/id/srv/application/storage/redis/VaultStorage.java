package pt.fct.nova.id.srv.application.storage.redis;

import redis.clients.jedis.Jedis;

import java.util.Map;

public class VaultStorage {

    public static void saveSecrets(String storeID, Map<String, String> secrets) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(storeID, secrets);
        }
    }

    public static Map<String, String> getSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(storeID);
        }
    }

    public static void deleteSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(storeID);
        }
    }

    public static boolean exists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID);
        }
    }
}
