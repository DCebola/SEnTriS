package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocksClient {

    private final static String STORE_LOCK = "LS".concat(System.getenv("BASIC_SEPARATOR")).concat("%s");
    private final static String USER_LOCK = "LU".concat(System.getenv("BASIC_SEPARATOR")).concat("%s");

    private final static String LOCK_TIMEOUT = System.getenv("LOCK_TIMEOUT");
    private static final int LOCK_TRIES = Integer.parseInt(System.getenv("LOCK_TRIES"));
    private static final long LOCK_SLEEP = Long.parseLong(System.getenv("LOCK_SLEEP"));
    private final static String LOCK_SCRIPT = """
            local val = redis.call('setnx', KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call('expire', KEYS[1], ARGV[2])
            return 1
            """;
    private final static String UNLOCK_SCRIPT = """
            if redis.call("get",KEYS[1]) == ARGV[1] then
                return redis.call("del",KEYS[1])
            else
                return 0
            end
            """;

    public static synchronized String acquireUserLock(String username) throws InterruptedException, TooManyLockRetriesException {
        return acquireLock(String.format(USER_LOCK, username));
    }

    public static synchronized String acquireStoreLock(String storeID) throws InterruptedException, TooManyLockRetriesException {
        return acquireLock(String.format(STORE_LOCK, storeID));
    }

    public static synchronized void releaseUserLock(String username, String lockID) {
        releaseLock(String.format(USER_LOCK, username), lockID);
    }

    public static boolean checkIfStoreLockExists(String storeID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(STORE_LOCK, storeID)).equals(lockID);
        }
    }

    public static synchronized void releaseStoreLock(String storeID, String lockID) {
        releaseLock(String.format(STORE_LOCK, storeID), lockID);
    }

    private static synchronized String acquireLock(String key) throws InterruptedException, TooManyLockRetriesException {
        String uuid = UUID.randomUUID().toString();
        int tries = LOCK_TRIES;
        while (tries > 0) {
            if (tryToAcquireLock(key, uuid) != 1L)
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
            args.add(LOCK_TIMEOUT);
            return (Long) jedis.eval(LOCK_SCRIPT, keys, args);
        }
    }

    private static synchronized void releaseLock(String key, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(1);
            keys.add(key);
            args.add(lockID);
            jedis.eval(UNLOCK_SCRIPT, keys, args);
        }
    }
}
