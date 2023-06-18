package pt.fct.nova.id.srv.application.storage.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisEncryptedStorageEngineV1 extends RedisEncryptedStorageEngine {

    @Override
    public void delete(String triplestoreID, Set<byte[]> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID, new String(trapdoor, StandardCharsets.UTF_8))));
            t.exec();
        }
    }

}
