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

import static pt.fct.nova.id.srv.application.Utils.generateID;

public class ProxyStorageV2 extends ProxyStorage {
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final int NUM_THREADS = Integer.parseInt(System.getenv("NUM_THREADS"));

    public static BindingsTableV2 search(DGKEqKey key, Var[] vars, Map<Var, String> searches) throws SPARQLExecutionException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTableV2 res = new MemBindingsTableV2(vars);
            System.out.println("Search: " + Arrays.toString(vars) + " | " + searches.entrySet());
            Pipeline p = jedis.pipelined();
            List<Response<List<String>>> responses = new ArrayList<>(searches.size());
            for (Var var : vars)
                responses.add(p.lrange(searches.get(var), 0, -1));

            p.sync();
            Map<Var, List<String>> searchResults = new HashMap<>();

            for (int i = 0; i < vars.length; i++)
                searchResults.put(vars[i], responses.get(i).get());

            Map<Var, Set<BigInteger>> groupedEqTags = new ConcurrentHashMap<>();

            System.out.println("creating service");

            List<Thread> threads = new ArrayList<>(NUM_THREADS);
            int total = searchResults.get(vars[0]).size();
            int batchSize = total / NUM_THREADS;
            int totalBatches;
            int currentLimit;
            if (total < NUM_THREADS) {
                totalBatches = 1;
                currentLimit = total;
            } else {
                batchSize = total / NUM_THREADS;
                totalBatches = NUM_THREADS;
                currentLimit = batchSize + (total % NUM_THREADS);
            }
            int offset = 0;
            for (int t = 0; t < totalBatches; t++) {
                int finalCurrentLimit = currentLimit;
                int finalOffset = offset;
                threads.add(new Thread(() -> {
                    for (int i = finalOffset; i < finalCurrentLimit; i++) {
                        String p_idx = generateID();
                        for (Var var : vars) {
                            BigInteger eqTag = DGKEqUtils.mod(key, new BigInteger(base64Decoder.decode(searchResults.get(var).get(i))));
                            try {
                                res.add(p_idx, var, groupEqTag(key, groupedEqTags, var, eqTag));
                            } catch (HomomorphicException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }));
                offset = currentLimit;
                currentLimit += batchSize;
            }
            threads.forEach(Thread::start);
            for (Thread thread : threads)
                thread.join();
            System.out.println("Built Table: " + Arrays.toString(vars) + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            throw new SPARQLExecutionException(e.getMessage());
        }
    }

    private static BigInteger groupEqTag(DGKEqKey key, Map<Var, Set<BigInteger>> groupedEqTags, Var var, BigInteger newEqTag) throws HomomorphicException {
        Set<BigInteger> eqTags = groupedEqTags.get(var);
        if (eqTags == null) {
            eqTags = ConcurrentHashMap.newKeySet();
            eqTags.add(newEqTag);
            groupedEqTags.put(var, eqTags);
            return newEqTag;
        }

        for (BigInteger eqTag : eqTags) {
            if (DGKEqUtils.equals(key, eqTag, newEqTag))
                return eqTag;
        }
        groupedEqTags.get(var).add(newEqTag);
        return newEqTag;
    }

}
