package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocksClient {

    private final static String LOCK = "L".concat(System.getenv("BASIC_SEPARATOR")).concat("%s");
    private final static String LOCK_LIFETIME = System.getenv("LOCK_LIFETIME");
    private static final int LOCK_TRIES = Integer.parseInt(System.getenv("LOCK_TRIES"));
    private static final long LOCK_SLEEP = Long.parseLong(System.getenv("LOCK_SLEEP"));
    private final static String LOCK_SCRIPT = """
            local val = redis.call('setnx', KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call('expire', KEYS[1], ARGV[2])
            return 1""";
    private final static String UNLOCK_SCRIPT = """
            if redis.call("get",KEYS[1]) == ARGV[1] then
                return redis.call("del",KEYS[1])
            else
                return 0
            end
            """;


    public static synchronized String acquireLock(String key) throws InterruptedException, TooManyLockRetriesException {
        String uuid = UUID.randomUUID().toString();
        int tries = LOCK_TRIES;
        while (tries > 0) {
            if (tryToAcquireLock(String.format(LOCK, key), uuid) != 1L)
                return uuid;
            tries--;
            Thread.sleep(LOCK_SLEEP);
        }
        throw new TooManyLockRetriesException();
    }

    private static Long tryToAcquireLock(String key, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(2);
            keys.add(key);
            args.add(lockID);
            args.add(LOCK_LIFETIME);
            return (Long) jedis.eval(LOCK_SCRIPT, keys, args);
        }
    }

    public static synchronized void releaseLock(String key, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(1);
            keys.add(String.format(LOCK, key));
            args.add(lockID);
            jedis.eval(UNLOCK_SCRIPT, keys, args);
        }
    }
}
