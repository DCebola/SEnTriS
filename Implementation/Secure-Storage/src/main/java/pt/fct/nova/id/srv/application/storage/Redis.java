package pt.fct.nova.id.srv.application.storage;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Redis {

    private static JedisPool instance;

    private synchronized static JedisPool getInstance() {
        if (instance != null)
            return instance;
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        instance = new JedisPool(
                poolConfig,
                System.getenv("REDIS_HOST"),
                Integer.parseInt(System.getenv("REDIS_PORT")),
                1000,
                true);

        return instance;
    }

    public synchronized static JedisPool getCachePool() {
        return getInstance();
    }
}