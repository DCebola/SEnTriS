package pt.fct.nova.id.srv.application.storage;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;

public class Redis {

    private static JedisPool instance;

    private synchronized static JedisPool getInstance() throws IOException {
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

    public synchronized static JedisPool getCachePool() throws IOException {
        return getInstance();
    }
}