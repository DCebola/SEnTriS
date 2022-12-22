package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import pt.fct.nova.id.srv.application.storage.iri_tables.MemIRITable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class RedisEncryptedStorageEngine implements EncryptedStorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            ScanParams params = new ScanParams();
            params.match(String.format(STORE_DATA_PATTERN, triplestoreID));
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
    public void delete(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            trapdoors.forEach(trapdoor -> t.del(String.format(KEY_FORMAT, triplestoreID, trapdoor)));
            t.exec();
        }
    }

    @Override
    public void save(String triplestoreID, Map<String, String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (Map.Entry<String, String> entry : encryptedNodes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                t.set(String.format(KEY_FORMAT, triplestoreID, key), value);
            }
            t.exec();
        }
    }

    @Override
    public List<String> search(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<String> res = new LinkedList<>();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            System.out.println(trapdoors.size());
            trapdoors.forEach(key -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, key))));
            p.sync();
            for (Response<String> r : responses)
                res.add(r.get());
            return res;
        }
    }
}
