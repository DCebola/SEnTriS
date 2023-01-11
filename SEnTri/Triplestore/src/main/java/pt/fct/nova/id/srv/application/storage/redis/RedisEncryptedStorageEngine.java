package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;

public class RedisEncryptedStorageEngine implements EncryptedStorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    private static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            Redis.scan(jedis, TRIPLESTORE_DATA_PATTERN).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void delete(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID, trapdoor)));
            t.exec();
        }
    }

    @Override
    public void save(String triplestoreID, Map<String, String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (Map.Entry<String, String> entry : encryptedNodes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                t.set(String.format(KEY_FORMAT, triplestoreID, key), value);
            }
            t.exec();
        }
    }

    @Override
    public void swap(String triplestoreID, Map<String, String> values) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Map<String, Response<String>> swaps = new HashMap<>(values.size());
            for (String key : values.keySet())
                swaps.put(key, p.get(key));
            p.sync();
            Transaction t = jedis.multi();
            for (String key : swaps.keySet()) {
                t.set(String.format(KEY_FORMAT, triplestoreID, values.get(key)), swaps.get(key).get());
                t.del(String.format(KEY_FORMAT, triplestoreID, key));
            }
            t.exec();
        }
    }

    @Override
    public List<String> search(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, key))));
            System.out.println("SEARCH: " + trapdoors.size());
            p.sync();
            List<String> res = new ArrayList<>(trapdoors.size());
            for (Response<String> r : responses) res.add(r.get());
            return res;
        }
    }
}
