package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngineV2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisEncryptedStorageEngineV2 extends RedisEncryptedStorageEngine implements EncryptedStorageEngineV2 {
    @Override
    public void delete(String triplestoreID, List<byte[]> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<byte[]>> responses = new LinkedList<>();
            trapdoors.forEach(trapdoor -> responses.add(
                    p.get(String.format(KEY_FORMAT, triplestoreID, new String(trapdoor, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8))));
            p.sync();
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID,
                    new String(trapdoor, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)));
            responses.forEach(response -> t.del(String.format(KEY_FORMAT, triplestoreID,
                    new String(response.get(), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8)));
            t.exec();
        }
    }

    public List<byte[]> maskedSearch(String triplestoreID, List<byte[]> trapdoors, BigInteger mask) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<byte[]>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID,
                    new String(key, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8))));
            System.out.println("SEARCH: " + trapdoors.size());
            p.sync();
            List<byte[]> res = new ArrayList<>(trapdoors.size());
            byte[] value;
            for (Response<byte[]> r : responses) {
                value = r.get();
                if (value != null)
                    value = new BigInteger(value).multiply(mask).toByteArray();
                res.add(value);
            }
            return res;
        }
    }
}
