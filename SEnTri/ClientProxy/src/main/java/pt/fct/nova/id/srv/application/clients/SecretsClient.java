package pt.fct.nova.id.srv.application.clients;

import com.google.gson.Gson;
import pt.fct.nova.id.srv.application.protocols.EncryptionProtocol;
import java.util.List;


public class SecretsClient {
    //TODO: Use httpsClient,
    private static final Gson gson = new Gson();
    public static final int PROTOCOL_VERSION = 0;
    public static final int P1_KEY_1 = 1;
    public static final int P1_KEY_2 = 2;
    public static final int P1_KEY_3 = 3;
    public static final int P1_IV = 4;

    public static void saveProtocolSecrets(EncryptionProtocol protocol) {
        /*
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
         */
    }

    public static List<String> getProtocolSecrets(String storeID) {
         /*
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.lrange(storeID, 0, -1);
        }
        */
        return null;
    }

    public static boolean exists(String storeID) {
         /*
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(storeID);
        }
        */
        return false;
    }
}
