package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;

public class SecretsClient {
    //TODO: Using https

    public static boolean exists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID);
        }
    }
}
