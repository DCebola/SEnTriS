package pt.fct.nova.id.srv.application.clients;

import pt.fct.nova.id.srv.application.UUIDUtils;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.redis.Redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

public class LocksClient {
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String TRIPLESTORE_LOCK = "LT".concat(BASIC_SEPARATOR).concat("%s");
    private final static String USER_LOCK = "LU".concat(BASIC_SEPARATOR).concat("%s");
    private final static String USER_TRIPLESTORE_LOCK = "UTL".concat(BASIC_SEPARATOR).concat("%s")
            .concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_ALL_LOCKS_PATTERN = "UTL".concat(BASIC_SEPARATOR)
            .concat("%s").concat(BASIC_SEPARATOR).concat("*");
    private static final String USER_TRIPLESTORE_LOCK_PATTERN = "UTL".concat(BASIC_SEPARATOR)
            .concat("%s").concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("*");
    private final static String LOCK_LIFETIME = System.getenv("LOCK_LIFETIME");
    private static final int LOCK_TRIES = Integer.parseInt(System.getenv("LOCK_TRIES"));
    private static final long LOCK_SLEEP = Long.parseLong(System.getenv("LOCK_SLEEP"));

    private static final int UTL_USER_POS = 1;
    private static final int UTL_TRIPLESTORE_POS = 2;
    private static final int UTL_LOCK_POS = 3;
    private final static String USER_LOCK_SCRIPT = """
            local val = redis.call("setnx", KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call("expire", KEYS[1], ARGV[2])
            return 1
            """;

    private final static String USER_UNLOCK_SCRIPT = """
            if redis.call("get",KEYS[1]) == ARGV[1] then
                return redis.call("del",KEYS[1])
            end
            return 0
            """;
    private final static String TRIPLESTORE_LOCK_SCRIPT = """
            local val = redis.call("setnx", KEYS[1], ARGV[1])
            if val == 0 then
                return 0
            end
            redis.call("setnx", KEYS[2], 0)
            redis.call("expire", KEYS[2], ARGV[2])
            redis.call("expire", KEYS[1], ARGV[2])
            return 1
            """;

    private final static String TRIPLESTORE_UNLOCK_SCRIPT = """
            if redis.call("get",KEYS[1]) == ARGV[1] then
                redis.call("del",KEYS[1])
                redis.call("del",KEYS[2])
                return 1
            end
            return 0
            """;

    public static synchronized String acquireUserLock(String username) throws InterruptedException, TooManyLockRetriesException {
        String key = String.format(USER_LOCK, username);
        String uuid = UUIDUtils.generateID();;
        List<String> keys = new ArrayList<>(1);
        List<String> args = new ArrayList<>(2);
        keys.add(key);
        args.add(uuid);
        args.add(LOCK_LIFETIME);
        return execLockScript(USER_LOCK_SCRIPT, uuid, keys, args);
    }

    public static synchronized void releaseUserLock(String username, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(1);
            List<String> args = new ArrayList<>(1);
            keys.add(String.format(USER_LOCK, username));
            args.add(lockID);
            jedis.eval(USER_UNLOCK_SCRIPT, keys, args);
        }
    }


    public static synchronized String acquireTriplestoreLock(String username, String triplestoreID) throws InterruptedException, TooManyLockRetriesException {
        String uuid = UUIDUtils.generateID();;
        List<String> keys = new ArrayList<>(2);
        List<String> args = new ArrayList<>(2);
        keys.add(String.format(TRIPLESTORE_LOCK, triplestoreID));
        keys.add(String.format(USER_TRIPLESTORE_LOCK, username, triplestoreID, uuid));
        args.add(uuid);
        args.add(LOCK_LIFETIME);
        return execLockScript(TRIPLESTORE_LOCK_SCRIPT, uuid, keys, args);

    }


    public static boolean checkIfTriplestoreLockExists(String triplestoreID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String l = jedis.get(String.format(TRIPLESTORE_LOCK, triplestoreID));
            System.out.println(l);
            System.out.println(lockID);
            return l.equals(lockID);
        }
    }

    public static synchronized void releaseTriplestoreLock(String username, String triplestoreID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(2);
            List<String> args = new ArrayList<>(1);
            keys.add(String.format(TRIPLESTORE_LOCK, triplestoreID));
            keys.add(String.format(USER_TRIPLESTORE_LOCK, username, triplestoreID, lockID));
            args.add(lockID);
            Long res = (Long) jedis.eval(TRIPLESTORE_UNLOCK_SCRIPT, keys, args);
            System.out.println("Deletion res:" + res);
        }
    }

    private static void releaseTriplestoreLock(Transaction t, String username, String triplestoreID, String lockID) {
        List<String> keys = new ArrayList<>(2);
        List<String> args = new ArrayList<>(1);
        keys.add(String.format(TRIPLESTORE_LOCK, triplestoreID));
        keys.add(String.format(USER_TRIPLESTORE_LOCK, username, triplestoreID, lockID));
        args.add(lockID);
        t.eval(TRIPLESTORE_UNLOCK_SCRIPT, keys, args);
    }

    public static synchronized void deleteUserTriplestoreLock(String username, String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            deleteUserLocks(jedis, Redis.scan(jedis, String.format(USER_TRIPLESTORE_LOCK_PATTERN, username, triplestoreID)));
        }
    }

    public static synchronized void deleteAllUserLocks(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            deleteUserLocks(jedis, Redis.scan(jedis, String.format(USER_ALL_LOCKS_PATTERN, username)));
        }
    }

    private static synchronized void deleteUserLocks(Jedis jedis, Set<String> lockIDs) {
        if (!lockIDs.isEmpty()) {
            Transaction t = jedis.multi();
            String[] values;
            for (String s : lockIDs) {
                values = s.split(BASIC_SEPARATOR);
                releaseTriplestoreLock(t, values[UTL_USER_POS], values[UTL_TRIPLESTORE_POS], values[UTL_LOCK_POS]);
            }
            t.exec();
        }
    }

    private static String execLockScript(String script, String lockID, List<String> keys, List<String> args) throws InterruptedException, TooManyLockRetriesException {
        int tries = LOCK_TRIES;
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            while (tries > 0) {
                Long res = (Long) jedis.eval(script, keys, args);
                System.out.println("Try n " + tries + "to acquire lock [" + lockID + "]: " + res);
                if (res == 1L)
                    return lockID;
                tries--;
                Thread.sleep(LOCK_SLEEP);
            }
            throw new TooManyLockRetriesException();
        }
    }
}
