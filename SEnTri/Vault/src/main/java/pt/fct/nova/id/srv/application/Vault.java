package pt.fct.nova.id.srv.application;

import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class Vault {

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
