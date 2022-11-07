package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.clients.redis.Redis;
import pt.fct.nova.id.srv.application.crypto.KeyStoreUtils;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import pt.fct.nova.id.srv.application.protocols.Protocol1;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.crypto.SecretKey;
import java.security.KeyStoreException;
import java.util.List;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static pt.fct.nova.id.srv.application.clients.Utils.generateID;

public class StorageClient {
    public static final int PROTOCOL_VERSION = 0;
    public static final int P1_KEY_1 = 1;
    public static final int P1_KEY_2 = 2;
    public static final int P1_KEY_3 = 3;
    public static final int P1_IV = 4;
    public static void saveProtocolSecrets(EncryptionProtocol protocol, char[] password) throws KeyStoreException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (protocol instanceof Protocol1 p) {
                t.lpush(p.getStoreID(), ProtocolVersion.V1.toString());
                saveKey(t, p.getStoreID(), p.getK1(), generateID(), password);
                saveKey(t, p.getStoreID(), p.getK3(), generateID(), password);
                saveKey(t, p.getStoreID(), p.getK2(), generateID(), password);
                t.lpush(p.getStoreID(), encodeBase64URLSafeString(p.getIv()));
            }
            t.exec();
        }
    }

    private static void saveKey(Transaction t, String storeID, SecretKey key, String alias, char[] password) throws KeyStoreException {
        KeyStoreUtils.saveSecretKey(alias, password, key);
        t.lpush(storeID, alias);
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
