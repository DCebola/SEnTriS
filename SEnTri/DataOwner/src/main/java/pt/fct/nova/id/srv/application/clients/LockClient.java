package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.clients.redis.Redis;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LockClient {

    private final static String LOCK = "L".concat(System.getenv("BASIC_SEPARATOR")).concat("%s");

    private final static String LOCK_TIMEOUT = System.getenv("LOCK_TIMEOUT");
    private static final int LOCK_TRIES = Integer.parseInt(System.getenv("LOCK_TRIES"));
    private static final long LOCK_SLEEP = Long.parseLong(System.getenv("LOCK_SLEEP"));
    private final static String LOCK_SCRIPT = "local val = redis.call('setnx', KEYS[1], ARGV[1])\n" +
            "if val == 0 then\n" +
            "    return 0\n" +
            "end\n" +
            "redis.call('expire', KEYS[1], ARGV[2])\n" +
            "return 1";
    private final static String UNLOCK_SCRIPT = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
            "    return redis.call(\"del\",KEYS[1])\n" +
            "else\n" +
            "    return 0\n" +
            "end\n";

    public static synchronized String acquireLock(String storeID) throws InterruptedException {
        String uuid = UUID.randomUUID().toString();
        int tries = LOCK_TRIES;
        while (tries > 0) {
            if (tryToAcquireLock(storeID, uuid) != 1L)
                return uuid;
            tries--;
            Thread.sleep(LOCK_SLEEP);
        }
        return null;
    }

    private static Long tryToAcquireLock(String storeID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(2);
            keys.add(String.format(LOCK, storeID));
            args.add(lockID);
            args.add(LOCK_TIMEOUT);
            return (Long) jedis.eval(LOCK_SCRIPT, keys, args);
        }
    }

    public static synchronized void releaseLock(String storeID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(1);
            keys.add(String.format(LOCK, storeID));
            args.add(lockID);
            jedis.eval(UNLOCK_SCRIPT, keys, args);
        }
    }
}
