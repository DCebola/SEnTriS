package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class RedisEncryptedStorageEngine implements EncryptedStorageEngine {
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    public static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            Redis.scan(jedis, String.format(TRIPLESTORE_DATA_PATTERN, triplestoreID).getBytes(StandardCharsets.UTF_8)).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void save(String triplestoreID, Map<byte[], byte[]> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (Map.Entry<byte[], byte[]> entry : encryptedNodes.entrySet()) {
                t.set(String.format(KEY_FORMAT, triplestoreID, new String(entry.getKey(), StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8), entry.getValue());
            }
            t.exec();
        }
    }

    @Override
    public List<byte[]> search(String triplestoreID, List<byte[]> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<byte[]>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(trapdoor ->
                    responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, new String(trapdoor, StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8))));

            p.sync();
            List<byte[]> res = new ArrayList<>(trapdoors.size());
            int total = 0;
            for (Response<byte[]> r : responses) {
                res.add(r.get());
                total++;
            }

            System.out.println("SEARCH: " + trapdoors.size() + " | " + "FOUND: " + total);
            return res;
        }
    }
}
