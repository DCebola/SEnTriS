package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class EncryptedStorageEngine implements pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void delete(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            ScanParams params = new ScanParams();
            params.match(String.format(STORE_DATA_PATTERN, storeID));
            String cursor = SCAN_POINTER_START;
            Set<String> collector = new HashSet<>();
            do {
                ScanResult<String> scanResult = jedis.scan(cursor, params);
                List<String> res = scanResult.getResult();
                collector.addAll(res);
                cursor = scanResult.getCursor();
            } while (!cursor.equals(SCAN_POINTER_START));
            Transaction t = jedis.multi();
            collector.forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void delete(String storeID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, storeID, trapdoor)));
            t.exec();
        }
    }

    @Override
    public void save(String storeID, Map<String, String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (Map.Entry<String, String> entry : encryptedNodes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                t.set(String.format(KEY_FORMAT, storeID, key), value);
            }
            t.exec();
        }
    }

    @Override
    public List<String> search(String storeID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<String> res = new LinkedList<>();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, storeID, key))));
            p.sync();
            String c;
            for (Response<String> r : responses) {
                c = r.get();
                if (c != null)
                    res.add(c);
            }
            return res;
        }
    }

    @Override
    public IRITable search(String storeID, Var var1, Var var2, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            MemIRITable res = new MemIRITable();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, storeID, key))));
            p.sync();
            String c1, c2, p_idx;
            for (int i = 0; i < responses.size(); i += 2) {
                c1 = responses.get(i).get();
                c2 = responses.get(i + 1).get();
                if (c1 != null && c2 != null) {
                    p_idx = generateID();
                    res.add(p_idx, var1, c1);
                    res.add(p_idx, var2, c2);
                }
            }
            return res;
        }
    }

    @Override
    public IRITable search(String storeID, Var var, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            MemIRITable res = new MemIRITable();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, storeID, key))));
            p.sync();
            String c;
            for (Response<String> r : responses) {
                c = r.get();
                if (c != null) {
                    res.add(generateID(), var, c);
                }
            }
            return res;
        }
    }
}
