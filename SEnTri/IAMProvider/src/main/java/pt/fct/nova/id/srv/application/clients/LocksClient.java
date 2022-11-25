package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.redis.Redis;
import pt.fct.nova.id.srv.application.redis.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import java.util.*;

public class LocksClient {
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String STORE_LOCK = "LS".concat(BASIC_SEPARATOR).concat("%s");
    private final static String USER_LOCK = "LU".concat(BASIC_SEPARATOR).concat("%s");
    private final static String USER_STORE_LOCK = "USL".concat(BASIC_SEPARATOR).concat("%s")
            .concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_ALL_LOCKS_PATTERN = "USL".concat(BASIC_SEPARATOR)
            .concat("%s").concat(BASIC_SEPARATOR).concat("*");
    private static final String USER_STORE_LOCK_PATTERN = "USL".concat(BASIC_SEPARATOR)
            .concat("%s").concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("*");
    private final static String LOCK_TIMEOUT = System.getenv("LOCK_TIMEOUT");
    private static final int LOCK_TRIES = Integer.parseInt(System.getenv("LOCK_TRIES"));
    private static final long LOCK_SLEEP = Long.parseLong(System.getenv("LOCK_SLEEP"));
    private static final int USL_STORE_POS = 2;
    private static final int USL_LOCK_POS = 3;
    private final static String USER_LOCK_SCRIPT = """
            local val = redis.call('setnx', KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call('expire', KEYS[1], ARGV[2])
            return 1
            """;
    private final static String STORE_LOCK_SCRIPT = """
            local val = redis.call('setnx', KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call('setnx', KEYS[2], 0)
            redis.call('expire', KEYS[2], ARGV[2])
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
        String key = String.format(USER_LOCK, username);
        String uuid = UUID.randomUUID().toString();
        List<String> keys = new ArrayList<>(1);
        List<String> args = new ArrayList<>(2);
        keys.add(key);
        args.add(uuid);
        args.add(LOCK_TIMEOUT);
        return execLockScript(USER_LOCK_SCRIPT, uuid, keys, args);
    }

    public static synchronized void releaseUserLock(String username, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(1);
            keys.add(String.format(USER_LOCK, username));
            args.add(lockID);
            jedis.eval(UNLOCK_SCRIPT, keys, args);
        }
    }


    public static synchronized String acquireStoreLock(String username, String storeID) throws InterruptedException, TooManyLockRetriesException {
        String uuid = UUID.randomUUID().toString();
        List<String> keys = new ArrayList<>(2);
        List<String> args = new ArrayList<>(2);
        keys.add(String.format(STORE_LOCK, storeID));
        keys.add(String.format(USER_STORE_LOCK, username));
        args.add(uuid);
        args.add(LOCK_TIMEOUT);
        return execLockScript(STORE_LOCK_SCRIPT, uuid, keys, args);

    }


    public static boolean checkIfStoreLockExists(String storeID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(STORE_LOCK, storeID)).equals(lockID);
        }
    }

    public static synchronized void releaseStoreLock(String username, String storeID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            releaseStoreLock(t, storeID, lockID);
            t.del(String.format(USER_STORE_LOCK, username, storeID, lockID));
            t.exec();
        }
    }

    private static void releaseStoreLock(Transaction t, String storeID, String lockID) {
        List<String> keys = new ArrayList<>(1);
        List<String> args = new ArrayList<>(1);
        keys.add(String.format(STORE_LOCK, storeID));
        args.add(lockID);
        t.eval(UNLOCK_SCRIPT, keys, args);
    }

    public static synchronized void deleteUserStoreLock(String username, String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            deleteUserLocks(jedis, Utils.scan(jedis, String.format(USER_STORE_LOCK_PATTERN, username, storeID)));
        }
    }

    public static synchronized void deleteAllUserLocks(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            deleteUserLocks(jedis, Utils.scan(jedis, String.format(USER_ALL_LOCKS_PATTERN, username)));
        }
    }

    private static void deleteUserLocks(Jedis jedis, Set<String> lockIDs) {
        if (!lockIDs.isEmpty()) {
            Transaction t = jedis.multi();
            String[] values;
            for (String s : lockIDs) {
                values = s.split(BASIC_SEPARATOR);
                releaseStoreLock(t, values[USL_STORE_POS], values[USL_LOCK_POS]);
                t.del(s);
            }
            t.exec();
        }
    }

    private static String execLockScript(String script, String lockID, List<String> keys, List<String> args) throws InterruptedException, TooManyLockRetriesException {
        int tries = LOCK_TRIES;
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            while (tries > 0) {
                if ((Long) jedis.eval(script, keys, args) != 1L)
                    return lockID;
                tries--;
                Thread.sleep(LOCK_SLEEP);
            }
            throw new TooManyLockRetriesException();
        }
    }
}
