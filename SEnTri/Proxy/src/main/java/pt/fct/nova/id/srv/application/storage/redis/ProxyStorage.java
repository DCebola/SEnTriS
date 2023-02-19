package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV1;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTableV1;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import javax.crypto.*;
import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorage {
    private static final long SEARCH_DATA_LIFETIME = Long.parseLong(System.getenv("SEARCH_DATA_LIFETIME"));
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static void delete(Set<String> searchIDs) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            searchIDs.forEach(t::del);
            t.exec();
        }
    }

    public static String save(List<String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String uuid = generateID();
            for (String n : encryptedNodes) {
                if (n != null)
                    t.rpush(uuid, n);
            }
            t.expire(uuid, SEARCH_DATA_LIFETIME);
            t.exec();
            return uuid;
        }
    }

    public static BindingsTableV1 search(SecretKey key, Var[] vars, Map<Var, String> searches) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTableV1 res = new MemBindingsTableV1(vars);
            System.out.println("Search: " + Arrays.toString(vars) + " | " + searches.entrySet());
            Pipeline p = jedis.pipelined();
            List<Response<List<String>>> responses = new ArrayList<>(searches.size());
            for (Var var : vars)
                responses.add(p.lrange(searches.get(var), 0, -1));
            p.sync();
            Map<Var, List<String>> searchResults = new HashMap<>();

            for (int i = 0; i < vars.length; i++)
                searchResults.put(vars[i], responses.get(i).get());

            String p_idx;
            for (int i = 0; i < searchResults.get(vars[0]).size(); i++) {
                p_idx = generateID();
                for (Var var : vars)
                    res.add(p_idx, var, decrypt(key, searchResults.get(var).get(i)));
            }
            System.out.println("Built Table: " + Arrays.toString(vars) + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }

    private static String decrypt(SecretKey key, String ciphertext) throws AEADBadTagException {
        return base64Encoder.encodeToString(SymmetricEncryptionUtils.decrypt(key, base64Decoder.decode(ciphertext)));
    }
}
