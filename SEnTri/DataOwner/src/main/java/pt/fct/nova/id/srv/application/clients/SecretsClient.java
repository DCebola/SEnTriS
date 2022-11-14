package pt.fct.nova.id.srv.application.clients;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.clients.redis.Redis;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.crypto.SecretKey;
import java.util.List;


public class SecretsClient {
    private static final Gson gson = new Gson();
    public static final int PROTOCOL_VERSION = 0;
    public static final int P1_KEY_1 = 1;
    public static final int P1_KEY_2 = 2;
    public static final int P1_KEY_3 = 3;
    public static final int P1_IV = 4;

    public static void saveProtocolSecrets(EncryptionProtocol protocol) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (protocol instanceof Protocol1 p) {
                t.lpush(p.getStoreID(), ProtocolVersion.V1.toString());
                t.lpush(p.getStoreID(), gson.toJson(p.getK1(), SecretKey.class));
                t.lpush(p.getStoreID(), gson.toJson(p.getK2(), SecretKey.class));
                t.lpush(p.getStoreID(), gson.toJson(p.getK3(), SecretKey.class));
                t.lpush(p.getStoreID(), Base64.encodeBase64URLSafeString(p.getIv()));
            }
            t.exec();
        }
    }

    public static List<String> getProtocolSecrets(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.lrange(storeID, 0, -1);
        }
    }

    public static boolean exists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID);
        }
    }
}
