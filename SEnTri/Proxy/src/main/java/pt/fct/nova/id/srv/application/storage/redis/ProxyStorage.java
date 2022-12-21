package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.SymmetricCipher;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import javax.crypto.SecretKey;
import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorage {
    private static final long SEARCH_DATA_LIFETIME = Long.parseLong(System.getenv("SEARCH_DATA_LIFETIME"));
    private static final Random rnd = new Random();
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
            encryptedNodes.forEach(n -> t.rpush(uuid, n));
            t.expire(uuid, SEARCH_DATA_LIFETIME);
            t.exec();
            return uuid;
        }
    }

    public static IRITable search(SecretKey key, List<Var> vars, List<String> searchIDs) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            MemIRITable res = new MemIRITable();
            List<Response<List<String>>> responses = new ArrayList<>(vars.size());
            searchIDs.forEach(searchID -> responses.add(p.lrange(searchID, 0, -1)));
            p.sync();
            Map<Var, List<String>> searchResults = new HashMap<>();
            for (int i = 0; i < vars.size(); i++)
                searchResults.put(vars.get(i), responses.get(i).get());
            List<String> encryptedNodes = searchResults.get(vars.get(rnd.nextInt(vars.size())));
            String p_idx;
            for (int i = 0; i < encryptedNodes.size(); i++) {
                p_idx = generateID();
                for (Var var : vars)
                    res.add(p_idx, var, base64Encoder.encodeToString(SymmetricCipher.decrypt(key, base64Decoder.decode(searchResults.get(var).get(i)))));
            }
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }
}
