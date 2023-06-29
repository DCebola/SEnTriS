package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisEncryptedStorageEngineV1 extends RedisEncryptedStorageEngine {

    @Override
    public byte[] commitDelete(String triplestoreID, Set<byte[]> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            byte[] id = Utils.generateID();
            trapdoors.forEach(trapdoor -> t.sadd(id, String.format(KEY_FORMAT, triplestoreID, new String(trapdoor, StandardCharsets.UTF_8))
                    .getBytes(StandardCharsets.UTF_8)));
            t.expire(id, COMMIT_LIFETIME);
            t.exec();
            return id;
        }
    }


}
