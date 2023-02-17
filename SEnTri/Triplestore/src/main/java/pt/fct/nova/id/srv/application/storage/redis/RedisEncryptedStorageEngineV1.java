package pt.fct.nova.id.srv.application.storage.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;

public class RedisEncryptedStorageEngineV1 extends RedisEncryptedStorageEngine {

    @Override
    public void delete(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID, trapdoor)));
            t.exec();
        }
    }

    @Override
    public void swap(String triplestoreID, Map<String, String> values) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Map<String, Response<String>> swaps = new HashMap<>(values.size());
            for (String key : values.keySet())
                swaps.put(key, p.get(String.format(KEY_FORMAT, triplestoreID, key)));
            p.sync();
            Transaction t = jedis.multi();
            for (String key : swaps.keySet()) {
                t.set(String.format(KEY_FORMAT, triplestoreID, values.get(key)), swaps.get(key).get());
                t.del(String.format(KEY_FORMAT, triplestoreID, key));
            }
            t.exec();
        }
    }
}
