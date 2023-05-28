package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.SymmetricEncryptionUtils;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV1;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTableV1;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import javax.crypto.SecretKey;
import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorageV1 extends ProxyStorage {
    public static BindingsTableV1 search(SecretKey key, Var[] vars, Map<Var, byte[]> searches) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTableV1 res = new MemBindingsTableV1(vars);
            System.out.println("Search: " + Arrays.toString(vars) + " | " + searches.entrySet());
            Pipeline p = jedis.pipelined();
            List<Response<List<byte[]>>> responses = new ArrayList<>(searches.size());
            for (Var var : vars)
                responses.add(p.lrange(searches.get(var), 0, -1));
            p.sync();
            Map<Var, List<byte[]>> searchResults = new HashMap<>();

            for (int i = 0; i < vars.length; i++)
                searchResults.put(vars[i], responses.get(i).get());

            byte[] p_idx;
            for (int i = 0; i < searchResults.get(vars[0]).size(); i++) {
                p_idx = generateID();
                for (Var var : vars)
                    res.add(p_idx, var, SymmetricEncryptionUtils.decrypt(key, searchResults.get(var).get(i)));
            }
            System.out.println("Built Table: " + Arrays.toString(vars) + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }
}
