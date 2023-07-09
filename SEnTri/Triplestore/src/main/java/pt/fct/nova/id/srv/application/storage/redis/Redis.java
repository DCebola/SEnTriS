package pt.fct.nova.id.srv.application.storage.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class Redis {

    private static JedisPool instance;

    private synchronized static JedisPool getInstance() {
        if (instance != null)
            return instance;
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(Integer.parseInt(System.getenv("REDIS_MAX_TOTAL")));
        poolConfig.setMaxIdle(Integer.parseInt(System.getenv("REDIS_MAX_IDLE")));
        poolConfig.setMinIdle(Integer.parseInt(System.getenv("REDIS_MIN_IDLE")));
        poolConfig.setTestOnBorrow(Boolean.parseBoolean(System.getenv("REDIS_TEST_ON_BORROW")));
        poolConfig.setTestOnReturn(Boolean.parseBoolean(System.getenv("REDIS_TEST_ON_RETURN")));
        poolConfig.setTestWhileIdle(Boolean.parseBoolean(System.getenv("REDIS_TEST_WHILE_IDLE")));
        poolConfig.setNumTestsPerEvictionRun(Integer.parseInt(System.getenv("REDIS_TESTS_PER_EVICTION_RUN")));
        poolConfig.setBlockWhenExhausted(Boolean.parseBoolean(System.getenv("REDIS_BLOCK_WHEN_EXHAUSTED")));

        instance = new JedisPool(
                poolConfig,
                System.getenv("REDIS_HOST"),
                Integer.parseInt(System.getenv("REDIS_PORT")),
                Integer.parseInt(System.getenv("REDIS_TIMEOUT")),
                true);

        return instance;
    }

    public synchronized static JedisPool getCachePool() {
        return getInstance();
    }

    public static Set<String> scan(Jedis jedis, String scanPattern){
        String cursor = SCAN_POINTER_START;
        ScanParams params = new ScanParams();
        params.match(scanPattern);
        Set<String> res = new HashSet<>();
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, params);
            res.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!cursor.equals(SCAN_POINTER_START));
        return res;
    }
}