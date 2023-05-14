package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngineV2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.math.BigInteger;
import java.util.*;

public class RedisEncryptedStorageEngineV2 extends RedisEncryptedStorageEngine implements EncryptedStorageEngineV2 {

    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();


    @Override
    public void delete(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses = new LinkedList<>();
            trapdoors.forEach(trapdoor -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, trapdoor))));
            p.sync();
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID, trapdoor)));
            responses.forEach(response -> t.del(String.format(KEY_FORMAT, triplestoreID, response.get())));
            t.exec();
        }
    }

    public List<String> maskedSearch(String triplestoreID, List<String> trapdoors, BigInteger mask) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, key))));
            System.out.println("SEARCH: " + trapdoors.size());
            p.sync();
            List<String> res = new ArrayList<>(trapdoors.size());
            String value;
            System.out.println("Mask: " + base64Encoder.encodeToString(mask.toByteArray()));
            for (Response<String> r : responses) {
                value = r.get();
                if (value != null) {
                    value = base64Encoder.encodeToString(new BigInteger(base64Decoder.decode(value)).multiply(mask).toByteArray());
                    System.out.println(" - " + value);
                }
                res.add(value);
            }
            return res;
        }
    }
}
