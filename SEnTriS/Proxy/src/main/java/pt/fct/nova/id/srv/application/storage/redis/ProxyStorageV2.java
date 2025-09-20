package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqUtils;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.Bytes;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTable;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.math.BigInteger;
import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorageV2 extends ProxyStorage {

    public static BindingsTable search(DGKEqKey key, Var[] vars, Map<Var, String> searches, byte[] executionID) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTable res = new MemBindingsTable(vars);
            System.out.println("Search: " + Arrays.toString(vars) + " | " + searches.entrySet());
            Pipeline p = jedis.pipelined();
            List<Response<List<byte[]>>> responses = new ArrayList<>(searches.size());
            for (Var var : vars)
                responses.add(p.lrange(getBase64Decoder().decode(searches.get(var)), 0, -1));

            p.sync();
            Map<Var, List<byte[]>> searchResults = new HashMap<>();
            for (int i = 0; i < vars.length; i++)
                searchResults.put(vars[i], responses.get(i).get());

            p = jedis.pipelined();
            Map<byte[], byte[]> eqTags = new HashMap<>(searchResults.size());
            Bytes p_idx;
            for (int i = 0; i < searchResults.get(vars[0]).size(); i++) {
                p_idx = new Bytes(generateID());
                for (Var var : vars) {
                    byte[] eqTag = searchResults.get(var).get(i);
                    byte[] binding = DGKEqUtils.removeRNDLayer(key, new BigInteger(eqTag));
                    eqTags.putIfAbsent(binding, eqTag);
                    res.add(p_idx, var, new Bytes(binding));
                }
            }
            for (Map.Entry<byte[], byte[]> entry : eqTags.entrySet()) {
                byte[] binding = entry.getKey();
                byte[] eqTag = entry.getValue();
                p.hset(executionID, binding, eqTag);
            }
            p.sync();
            System.out.println("Built Table: " + Arrays.toString(vars) + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }

    public static Map<byte[], byte[]> getEqTags(byte[] executionID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(executionID);
        }
    }

    public static void deleteEqTags(byte[] executionID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
           jedis.del(executionID);
        }
    }
}
