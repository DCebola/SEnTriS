package pt.fct.nova.id.srv.application.storage_clients.redis;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class RedisClient {

    private static final Gson gson = new Gson();



    public static void put(String key, String value) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map get(String key) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(key), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
