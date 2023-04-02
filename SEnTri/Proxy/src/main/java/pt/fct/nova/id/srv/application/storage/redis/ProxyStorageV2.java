package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqKey;
import pt.fct.nova.id.srv.application.crypto.dgk.DGKEqUtils;
import pt.fct.nova.id.srv.application.crypto.dgk.HomomorphicException;
import pt.fct.nova.id.srv.application.query.execution.exceptions.SPARQLExecutionException;
import pt.fct.nova.id.srv.application.storage.tables.BindingsTableV2;
import pt.fct.nova.id.srv.application.storage.tables.MemBindingsTableV2;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorageV2 extends ProxyStorage {
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();

    public static BindingsTableV2 search(DGKEqKey key, Var[] vars, Map<Var, String> searches) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTableV2 res = new MemBindingsTableV2(vars);
            System.out.println("Search: " + Arrays.toString(vars) + " | " + searches.entrySet());
            Pipeline p = jedis.pipelined();
            List<Response<List<String>>> responses = new ArrayList<>(searches.size());
            for (Var var : vars)
                responses.add(p.lrange(searches.get(var), 0, -1));

            p.sync();
            Map<Var, List<BigInteger>> searchResults = new HashMap<>();
            for (int i = 0; i < vars.length; i++)
                searchResults.put(vars[i], responses.get(i).get().parallelStream()
                        .map(eqTag -> DGKEqUtils.mod(key, new BigInteger(base64Decoder.decode(eqTag))))
                        .collect(Collectors.toCollection(ArrayList::new)));


            Map<Var, Set<BigInteger>> groupedEqTags = new ConcurrentHashMap<>();
            for (Var var : vars)
                groupedEqTags.put(var, ConcurrentHashMap.newKeySet());
            String p_idx;
            for (int i = 0; i < searchResults.get(vars[0]).size(); i++) {
                p_idx = generateID();
                for (Var var : vars)
                    res.add(p_idx, var, findEqTagGroup(key, groupedEqTags.get(var), searchResults.get(var).get(i)));
            }
            System.out.println("Built Table: " + Arrays.toString(vars) + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }

    private static BigInteger findEqTagGroup(DGKEqKey key, Set<BigInteger> groupedEqTags, BigInteger newEqTag) {
        BigInteger res = groupedEqTags.parallelStream()
                .filter(item -> {
                    try {
                        return DGKEqUtils.equals(key, item, newEqTag);
                    } catch (HomomorphicException e) {
                        throw new RuntimeException(e);
                    }
                })
                .findAny()
                .orElse(null);
        if (res != null)
            return res;
        groupedEqTags.add(newEqTag);
        return newEqTag;
    }

}
