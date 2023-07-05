package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisEncryptedStorageEngineV1 extends RedisEncryptedStorageEngine {

    @Override
    public String commitDelete(String triplestoreID, Set<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String id = Utils.generateID();
            trapdoors.forEach(trapdoor -> t.lpush(id, String.format(KEY_FORMAT, triplestoreID, trapdoor, StandardCharsets.UTF_8)));
            t.expire(id, COMMIT_LIFETIME);
            t.exec();
            return id;
        }
    }


}
