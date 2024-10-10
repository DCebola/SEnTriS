package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngineV2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.math.BigInteger;
import java.util.*;

public class RedisEncryptedStorageEngineV2 extends RedisEncryptedStorageEngine implements EncryptedStorageEngineV2 {
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();

    public List<byte[]> maskedSearch(String triplestoreID, List<String> trapdoors, BigInteger mask, BigInteger n) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(trapdoor -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, trapdoor))));
            System.out.println("SEARCH: " + trapdoors.size());
            p.sync();
            List<byte[]> res = new ArrayList<>(trapdoors.size());
            String b64value;
            for (Response<String> r : responses) {
                b64value = r.get();
                if (b64value != null)
                    res.add(new BigInteger(base64Decoder.decode(b64value)).multiply(mask).mod(n).toByteArray());
            }
            return res;
        }
    }
}
