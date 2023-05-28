package pt.fct.nova.id.srv.application.storage.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorage {
    private static final long SEARCH_DATA_LIFETIME = Long.parseLong(System.getenv("SEARCH_DATA_LIFETIME"));

    public static void delete(Set<byte[]> searchIDs) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            searchIDs.forEach(t::del);
            t.exec();
        }
    }

    public static byte[] save(List<byte[]> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            byte[] uuid = generateID();
            for (byte[] n : encryptedNodes) {
                if (n != null)
                    t.rpush(uuid, n);
            }
            t.expire(uuid, SEARCH_DATA_LIFETIME);
            t.exec();
            return uuid;
        }
    }
}
