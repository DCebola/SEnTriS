package pt.fct.nova.id.srv.application;

import jakarta.ws.rs.core.NewCookie;

import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.redis.Redis;

import pt.fct.nova.id.srv.application.redis.Utils;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static pt.fct.nova.id.srv.presentation.api.dtos.Role.ADMIN;

public class IAMStorage {
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String COOKIE_PARAM = "session";
    private static final int SESSION_LIFETIME = Integer.parseInt(System.getenv("SESSION_LIFETIME"));
    private static final String SESSION = "S".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_PASSWORD = "UP".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_ROLE = "UR".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_OWNED_TRIPLESTORES = "UOT".concat(BASIC_SEPARATOR).concat("%s");
    private static final String TRIPLESTORE_READ_ACCESS = "TRA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String TRIPLESTORE_WRITE_ACCESS = "TWA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String TRIPLESTORE_OWNER = "TO".concat(BASIC_SEPARATOR).concat("%s");
    private static final String TRIPLESTORES_PATTERN = "TO".concat(BASIC_SEPARATOR).concat("*");
    ;
    private static final String TRIPLESTORE_PENDING_ACCESS_REQUESTS = "TPA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String PENDING_ACCESS_REQUEST = "PA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String PENDING_ROLE_REQUESTS = "PRR";
    private static final String PENDING_ROLE_REQUEST = "PR".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STATUS = "STATUS";
    private static final String ACCESS_TOKEN = "AT".concat(BASIC_SEPARATOR).concat("%s");
    private static final long ACCESS_TOKEN_LIFETIME = Integer.toUnsignedLong(Integer.parseInt(System.getenv("ACCESS_TOKEN_LIFETIME")));
    public static final String TOKEN_USER_FIELD = "USER";
    public static final String TOKEN_TRIPLESTORE_FIELD = "TRIPLESTORE";
    public static final String TOKEN_LOCK_FIELD = "LOCK";
    private static final int PENDING_REQUEST_USER_IDX = 0;
    private static final int PENDING_REQUEST_VALUE_IDX = 1;


    public static NewCookie cacheSession(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String key = String.format(SESSION, username);
            String uuid = UUID.randomUUID().toString();
            Transaction t = jedis.multi();
            t.del(key);
            t.set(key, uuid);
            t.expire(key, SESSION_LIFETIME);
            t.exec();
            return buildCookie(uuid);
        }
    }

    private static NewCookie buildCookie(String uid) {
        return new NewCookie.Builder(COOKIE_PARAM)
                .value(uid)
                .path("/")
                .secure(true)
                .httpOnly(true)
                .build();
    }

    public static String getSession(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(SESSION, username));
        }
    }

    public static boolean userExists(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<Boolean> r1 = p.exists(String.format(USER_PASSWORD, username));
            Response<Boolean> r2 = p.exists(String.format(USER_ROLE, username));
            p.sync();
            return r1.get() && r2.get();
        }
    }

    public static void saveUser(String username, String password, Role role) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, username), password);
            t.set(String.format(USER_ROLE, username), role.name());
            t.exec();
        }
    }

    public static boolean checkIfOwnsAny(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(String.format(USER_OWNED_TRIPLESTORES, username));
        }
    }


    public static void deleteUser(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(USER_PASSWORD, username));
            t.del(String.format(USER_ROLE, username));
            t.del(String.format(USER_OWNED_TRIPLESTORES, username));
            t.del(String.format(SESSION, username));
            t.exec();
        }
    }

    public static String getPassword(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(USER_PASSWORD, username));
        }
    }

    public static Role getRole(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return Role.fromString(jedis.get(String.format(USER_ROLE, username)));
        }
    }

    public static void setRole(String username, Role role) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(USER_ROLE, username), role.name());
        }
    }

    public static void saveRoleRequest(String username, Role role) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String requestID = UUID.randomUUID().toString();
            t.rpush(PENDING_ROLE_REQUESTS, requestID);
            t.rpush(String.format(PENDING_ROLE_REQUEST, requestID), username, role.name());
            t.exec();
        }
    }

    public static RoleRequest getPendingRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> requestData = jedis.lrange(String.format(PENDING_ROLE_REQUEST, requestID), 0, -1);
            if (requestData == null || requestData.isEmpty())
                return null;
            else return buildRoleRequestData(requestID, requestData);
        }
    }

    private static RoleRequest buildRoleRequestData(String requestID, List<String> requestData) {
        return new RoleRequest(requestID, requestData.get(PENDING_REQUEST_USER_IDX), Role.fromString(requestData.get(PENDING_REQUEST_VALUE_IDX)));
    }

    public static List<RoleRequest> getPendingRoleRequests() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<String> requestIDs = jedis.lrange(String.format(PENDING_ROLE_REQUESTS), 0, -1);
            List<Response<List<String>>> roleRequestsResponses = new ArrayList<>(requestIDs.size());
            List<RoleRequest> pendingRequests = new ArrayList<>(requestIDs.size());
            for (String requestID : requestIDs)
                roleRequestsResponses.add(p.lrange(String.format(PENDING_ROLE_REQUEST, requestID), 0, -1));
            p.sync();
            List<String> requestData;
            for (int i = 0; i < roleRequestsResponses.size(); i++) {
                Response<List<String>> response = roleRequestsResponses.get(i);
                requestData = response.get();
                if (requestData != null && !requestData.isEmpty())
                    pendingRequests.add(buildRoleRequestData(requestIDs.get(i), requestData));
            }
            return pendingRequests;
        }
    }

    public static void deleteRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.lrem(PENDING_ROLE_REQUESTS, 1, requestID);
            t.del(String.format(PENDING_ROLE_REQUEST, requestID));
            t.exec();
        }
    }

    public static boolean checkIfOwns(String username, String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(USER_OWNED_TRIPLESTORES, username), triplestoreID);
        }
    }

    public static String getOwner(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(TRIPLESTORE_OWNER, triplestoreID));
        }
    }

    public static boolean checkIfUserHasReadAccess(String username, String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID), username);
        }
    }

    public static boolean checkIfUserHasWriteAccess(String username, String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID), username);
        }
    }

    public static boolean storeAccessPolicyExists(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<Boolean> r1 = p.exists(String.format(TRIPLESTORE_OWNER, triplestoreID));
            Response<Boolean> r2 = p.exists(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID));
            Response<Boolean> r3 = p.exists(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID));
            p.sync();
            return r1.get() || r2.get() || r3.get();
        }
    }

    public static void saveTriplestoreAccessPolicy(String triplestoreID, String owner, Set<String> read, Set<String> write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.sadd(String.format(USER_OWNED_TRIPLESTORES, owner), triplestoreID);
            t.set(String.format(TRIPLESTORE_OWNER, triplestoreID), owner);
            for (String username : read)
                t.sadd(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID), username);
            for (String username : write)
                t.sadd(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID), username);
            t.exec();
        }
    }

    public static void updateTriplestoreOwner(String triplestoreID, String currentOwner, String newOwner) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(TRIPLESTORE_OWNER, triplestoreID), newOwner);
            t.srem(String.format(USER_OWNED_TRIPLESTORES, currentOwner), triplestoreID);
            t.sadd(String.format(USER_OWNED_TRIPLESTORES, newOwner), triplestoreID);
            t.exec();
        }
    }

    public static void deleteTriplestoreAccessPolicy(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> pendingRequests = jedis.lrange(String.format(TRIPLESTORE_PENDING_ACCESS_REQUESTS, triplestoreID), 0, -1);
            Transaction t = jedis.multi();
            t.del(String.format(TRIPLESTORE_OWNER, triplestoreID));
            t.del(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID));
            t.del(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID));
            t.del(String.format(TRIPLESTORE_PENDING_ACCESS_REQUESTS, triplestoreID));
            for (String requestID : pendingRequests)
                t.del(String.format(PENDING_ACCESS_REQUEST, requestID));
            t.exec();
        }
    }

    public static Set<String> getTriplestores(String username, boolean write, boolean read, boolean owns) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> triplestoreIDs = Utils.scan(jedis, TRIPLESTORES_PATTERN).stream().map(key -> key.split(BASIC_SEPARATOR)[1]).collect(Collectors.toSet());
            if (!write && !read && !owns)
                return triplestoreIDs;
            Pipeline p = jedis.pipelined();
            Set<String> filteredTriplestoreIDs = new HashSet<>();
            if (owns) {
                List<Response<String>> owners = new ArrayList<>(triplestoreIDs.size());
                for (String triplestoreID : triplestoreIDs)
                    owners.add(p.get(String.format(TRIPLESTORE_OWNER, triplestoreID)));
                p.sync();
                int i = 0;
                for (String triplestoreID : triplestoreIDs) {
                    if (username.equals(owners.get(i).get()))
                        filteredTriplestoreIDs.add(triplestoreID);
                    i++;
                }
                return filteredTriplestoreIDs;
            }
            String accessCheckKey;
            if (write)
                accessCheckKey = TRIPLESTORE_WRITE_ACCESS;
            else
                accessCheckKey = TRIPLESTORE_READ_ACCESS;

            List<Response<Boolean>> accessCheck = new ArrayList<>(triplestoreIDs.size());
            for (String triplestoreID : triplestoreIDs)
                accessCheck.add(p.sismember(String.format(accessCheckKey, triplestoreID), username));
            p.sync();
            int i = 0;
            for (String triplestoreID : triplestoreIDs) {
                if (accessCheck.get(i).get())
                    filteredTriplestoreIDs.add(triplestoreID);
                i++;
            }
            return filteredTriplestoreIDs;
        }
    }

    public static Set<String> getUserWithReadAccess(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.smembers(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID));
        }
    }

    public static Set<String> getUserWithWriteAccess(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.smembers(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID));
        }
    }

    public static void revokeAccess(String triplestoreID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (!write) {
                t.srem(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID), username);
                t.srem(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID), username);
            } else
                t.srem(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID), username);
            t.exec();
        }
    }

    public static void grantAccess(String triplestoreID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (write) {
                t.sadd(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID), username);
                t.sadd(String.format(TRIPLESTORE_WRITE_ACCESS, triplestoreID), username);
            } else
                t.sadd(String.format(TRIPLESTORE_READ_ACCESS, triplestoreID), username);
            t.exec();
        }
    }

    public static void saveAccessRequest(String triplestoreID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String requestID = UUID.randomUUID().toString();
            t.rpush(String.format(TRIPLESTORE_PENDING_ACCESS_REQUESTS, triplestoreID), requestID);
            t.rpush(String.format(PENDING_ACCESS_REQUEST, requestID), username, String.valueOf(write));
            t.exec();
        }
    }

    public static AccessRequest getPendingAccessRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> requestData = jedis.lrange(String.format(PENDING_ACCESS_REQUEST, requestID), 0, -1);
            if (requestData == null || requestData.isEmpty())
                return null;
            else return buildAccessRequestData(requestID, requestData);
        }

    }

    private static AccessRequest buildAccessRequestData(String requestID, List<String> requestData) {
        return new AccessRequest(requestID, requestData.get(PENDING_REQUEST_USER_IDX),
                Boolean.parseBoolean(requestData.get(PENDING_REQUEST_VALUE_IDX)));
    }

    public static List<AccessRequest> getPendingAccessRequests(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<String> requestIDs = jedis.lrange(String.format(TRIPLESTORE_PENDING_ACCESS_REQUESTS, triplestoreID), 0, -1);
            List<Response<List<String>>> storeRequestsResponses = new ArrayList<>(requestIDs.size());
            List<AccessRequest> pendingRequests = new ArrayList<>(requestIDs.size());
            for (String requestID : requestIDs)
                storeRequestsResponses.add(p.lrange(String.format(PENDING_ACCESS_REQUEST, requestID), 0, -1));
            p.sync();
            List<String> requestData;
            for (int i = 0; i < storeRequestsResponses.size(); i++) {
                Response<List<String>> response = storeRequestsResponses.get(i);
                requestData = response.get();
                if (requestData != null && !requestData.isEmpty())
                    pendingRequests.add(buildAccessRequestData(requestIDs.get(i), requestData));
            }
            return pendingRequests;
        }
    }

    public static void deleteAccessRequest(String triplestoreID, String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.lrem(String.format(TRIPLESTORE_PENDING_ACCESS_REQUESTS, triplestoreID), 1, requestID);
            t.del(String.format(PENDING_ACCESS_REQUEST, requestID));
            t.exec();
        }
    }


    public static String saveToken(String username, String store) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String uuid = UUID.randomUUID().toString();
            String key = String.format(ACCESS_TOKEN, uuid);
            Transaction t = jedis.multi();
            t.del(key);
            t.hset(key, TOKEN_USER_FIELD, username);
            t.hset(key, TOKEN_TRIPLESTORE_FIELD, store);
            t.expire(key, ACCESS_TOKEN_LIFETIME);
            t.exec();
            return uuid;
        }
    }

    public static void addLockToToken(String tokenID, String lockID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(String.format(ACCESS_TOKEN, tokenID), TOKEN_LOCK_FIELD, lockID);
        }
    }

    public static void deleteLockFromToken(String tokenID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hdel(String.format(ACCESS_TOKEN, tokenID), TOKEN_LOCK_FIELD);
        }
    }

    public static Map<String, String> getToken(String tokenID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(String.format(ACCESS_TOKEN, tokenID));
        }
    }

    public static void deleteAccessToken(String tokenID, Map<String, String> values) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String lockID = values.get(TOKEN_LOCK_FIELD);
            if (lockID != null)
                LocksClient.releaseTriplestoreLock(
                        values.get(TOKEN_USER_FIELD),
                        values.get(TOKEN_TRIPLESTORE_FIELD),
                        lockID);
            jedis.del(String.format(ACCESS_TOKEN, tokenID));
        }
    }

    public static void init(String defaultUsername, String defaultPass) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, defaultUsername), defaultPass);
            t.set(String.format(USER_ROLE, defaultUsername), ADMIN.toString());
            t.set(STATUS, String.valueOf(true));
            t.exec();
        }
    }

    public static boolean isInit() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(STATUS);
        }
    }
}
