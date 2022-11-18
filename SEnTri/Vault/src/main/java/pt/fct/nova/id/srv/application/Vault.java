package pt.fct.nova.id.srv.application;


import org.apache.jena.atlas.lib.NotImplemented;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

public class Vault {

    public static final int PROTOCOL_VERSION = 0;
    public static final int STORE_ID = 1;
    public static final int P1_KEY_1 = 2;
    public static final int P1_KEY_2 = 3;
    public static final int P1_KEY_3 = 4;
    public static final int P1_IV = 5;

    public static void saveSecrets(List<String> secrets) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String version = secrets.get(PROTOCOL_VERSION);
            String storeID = secrets.get(STORE_ID);
            switch (ProtocolVersion.fromString(version)) {
                case V1 -> {
                    Transaction t = jedis.multi();
                    t.lpush(storeID, version);
                    t.lpush(storeID, secrets.get(P1_KEY_1));
                    t.lpush(storeID, secrets.get(P1_KEY_2));
                    t.lpush(storeID, secrets.get(P1_KEY_3));
                    t.lpush(storeID, secrets.get(P1_IV));
                    t.exec();
                }
                case V2 -> {
                    //To be implemented.
                }
            }
        }
    }

    public static List<String> getSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.lrange(storeID, 0, -1);
        }
    }

    public static void deleteSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(storeID);
        }
    }

    public static boolean exists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID);
        }
    }
}
