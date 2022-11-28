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

public class IAMStore {
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String COOKIE_PARAM = "session";
    private static final int COOKIE_LIFETIME = Integer.parseInt(System.getenv("COOKIE_LIFETIME"));
    private static final String SESSION = "S".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_PASSWORD = "UP".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_ROLE = "UR".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_OWNED_STORES = "UOS".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_READ_ACCESS = "SRA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_WRITE_ACCESS = "SWA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_OWNER = "SO".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORES_PATTERN = "SO".concat(BASIC_SEPARATOR).concat("*");
    ;
    private static final String STORE_PENDING_ACCESS_REQUESTS = "SPA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String PENDING_ACCESS_REQUEST = "PA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String PENDING_ROLE_REQUESTS = "PRR";
    private static final String PENDING_ROLE_REQUEST = "PR".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STATUS = "STATUS";
    private static final String ACCESS_TOKEN = "AT".concat(BASIC_SEPARATOR).concat("%s");
    private static final long ACCESS_TOKEN_LIFETIME = Integer.toUnsignedLong(Integer.parseInt(System.getenv("ACCESS_TOKEN_LIFETIME")));
    public static final String TOKEN_USER_FIELD = "USER";
    public static final String TOKEN_STORE_FIELD = "STORE";
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
            t.expire(key, COOKIE_LIFETIME);
            t.exec();
            return buildCookie(uuid);
        }
    }

    private static NewCookie buildCookie(String uid) {
        return new NewCookie.Builder(COOKIE_PARAM)
                .value(uid)
                .maxAge(COOKIE_LIFETIME)
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
            t.set(String.format(USER_ROLE, username), role.toString());
            t.exec();
        }
    }

    public static boolean checkIfOwnsAny(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(String.format(USER_OWNED_STORES, username));
        }
    }


    public static void deleteUser(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(USER_PASSWORD, username));
            t.del(String.format(USER_ROLE, username));
            t.del(String.format(USER_OWNED_STORES, username));
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
            jedis.sadd(String.format(USER_ROLE, username), role.toString());
        }
    }

    public static void saveRoleRequest(String username, Role role) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String requestID = UUID.randomUUID().toString();
            t.lpush(PENDING_ROLE_REQUESTS, requestID);
            t.lpush(String.format(PENDING_ROLE_REQUEST, requestID), username, role.toString());
            t.exec();
        }
    }

    public static RoleRequest getPendingRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> requestData = jedis.lrange(String.format(PENDING_ROLE_REQUEST, requestID), 0, -1);
            if (requestData == null || requestData.isEmpty())
                return null;
            else return parseRoleRequestData(requestData);
        }
    }

    private static RoleRequest parseRoleRequestData(List<String> requestData) {
        return new RoleRequest(requestData.get(PENDING_REQUEST_USER_IDX), Role.fromString(requestData.get(PENDING_REQUEST_VALUE_IDX)));
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
            for (Response<List<String>> response : roleRequestsResponses) {
                requestData = response.get();
                if (requestData != null && !requestData.isEmpty())
                    pendingRequests.add(parseRoleRequestData(requestData));
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

    public static boolean checkIfOwns(String username, String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(USER_OWNED_STORES, username), storeID);
        }
    }

    public static String getOwner(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(STORE_OWNER, storeID));
        }
    }

    public static boolean checkIfUserHasReadAccess(String username, String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(STORE_READ_ACCESS, storeID), username);
        }
    }

    public static boolean checkIfUserHasWriteAccess(String username, String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.sismember(String.format(STORE_WRITE_ACCESS, storeID), username);
        }
    }

    public static boolean storeAccessPolicyExists(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<Boolean> r1 = p.exists(String.format(STORE_READ_ACCESS, storeID));
            Response<Boolean> r2 = p.exists(String.format(STORE_WRITE_ACCESS, storeID));
            p.sync();
            return r1.get() || r2.get();
        }
    }

    public static void saveStoreAccessPolicy(String storeID, String owner, Set<String> read, Set<String> write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.sadd(String.format(USER_OWNED_STORES, owner), storeID);
            t.set(String.format(STORE_OWNER, storeID), owner);
            for (String username : read)
                t.sadd(String.format(STORE_READ_ACCESS, storeID), username);
            for (String username : write)
                t.sadd(String.format(STORE_WRITE_ACCESS, storeID), username);
            t.exec();
        }
    }

    public static void updateStoreOwner(String storeID, String currentOwner, String newOwner) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(STORE_OWNER, storeID), newOwner);
            t.srem(String.format(USER_OWNED_STORES, currentOwner), storeID);
            t.sadd(String.format(USER_OWNED_STORES, newOwner), storeID);
            t.exec();
        }
    }

    public static void deleteStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> pendingRequests = jedis.lrange(String.format(STORE_PENDING_ACCESS_REQUESTS, storeID), 0, -1);
            Transaction t = jedis.multi();
            t.del(String.format(STORE_OWNER, storeID));
            t.del(String.format(STORE_READ_ACCESS, storeID));
            t.del(String.format(STORE_WRITE_ACCESS, storeID));
            t.del(String.format(STORE_PENDING_ACCESS_REQUESTS, storeID));
            for (String requestID : pendingRequests)
                t.del(String.format(PENDING_ACCESS_REQUEST, requestID));
            t.exec();
        }
    }

    public static Set<String> getStores(String username, boolean write, boolean read, boolean owns) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> storeIDs = Utils.scan(jedis, STORES_PATTERN).stream().map(key -> key.split(BASIC_SEPARATOR)[1]).collect(Collectors.toSet());
            if (!write && !read && !owns)
                return storeIDs;
            Pipeline p = jedis.pipelined();
            Set<String> filteredStoreIDs = new HashSet<>();
            if (owns) {
                List<Response<String>> owners = new ArrayList<>(storeIDs.size());
                for (String storeID : storeIDs)
                    owners.add(p.get(String.format(STORE_OWNER, storeID)));
                p.sync();
                int i = 0;
                for (String storeID : storeIDs) {
                    if (username.equals(owners.get(i).get()))
                        filteredStoreIDs.add(storeID);
                    i++;
                }
                return filteredStoreIDs;
            }
            String accessCheckKey;
            if (write)
                accessCheckKey = STORE_WRITE_ACCESS;
            else
                accessCheckKey = STORE_READ_ACCESS;

            List<Response<Boolean>> accessCheck = new ArrayList<>(storeIDs.size());
            for (String storeID : storeIDs)
                accessCheck.add(p.sismember(String.format(accessCheckKey, storeID), username));
            p.sync();
            int i = 0;
            for (String storeID : storeIDs) {
                if (accessCheck.get(i).get())
                    filteredStoreIDs.add(storeID);
                i++;
            }
            return filteredStoreIDs;
        }

    }

    public static void revokeAccess(String storeID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (!write) {
                t.srem(String.format(STORE_READ_ACCESS, storeID), username);
                t.srem(String.format(STORE_WRITE_ACCESS, storeID), username);
            } else
                t.srem(String.format(STORE_WRITE_ACCESS, storeID), username);
            t.exec();
        }
    }

    public static void grantAccess(String storeID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (write) {
                t.sadd(String.format(STORE_READ_ACCESS, username), storeID);
                t.sadd(String.format(STORE_WRITE_ACCESS, username), storeID);
            } else
                t.sadd(String.format(STORE_READ_ACCESS, username), storeID);
            t.exec();
        }
    }

    public static void saveAccessRequest(String storeID, String username, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String requestID = UUID.randomUUID().toString();
            t.lpush(String.format(STORE_PENDING_ACCESS_REQUESTS, storeID), requestID);
            t.lpush(String.format(PENDING_ACCESS_REQUEST, requestID), username, String.valueOf(write));
            t.exec();
        }
    }

    public static AccessRequest getPendingAccessRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> requestData = jedis.lrange(String.format(PENDING_ACCESS_REQUEST, requestID), 0, -1);
            if (requestData == null || requestData.isEmpty())
                return null;
            else return parseAccessRequestData(requestData);
        }

    }

    private static AccessRequest parseAccessRequestData(List<String> requestData) {
        return new AccessRequest(requestData.get(PENDING_REQUEST_USER_IDX),
                Boolean.parseBoolean(requestData.get(PENDING_REQUEST_VALUE_IDX)));
    }

    public static List<AccessRequest> getPendingAccessRequests(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<String> requestIDs = jedis.lrange(String.format(STORE_PENDING_ACCESS_REQUESTS, storeID), 0, -1);
            List<Response<List<String>>> storeRequestsResponses = new ArrayList<>(requestIDs.size());
            List<AccessRequest> pendingRequests = new ArrayList<>(requestIDs.size());
            for (String requestID : requestIDs)
                storeRequestsResponses.add(p.lrange(String.format(PENDING_ACCESS_REQUEST, requestID), 0, -1));
            p.sync();
            List<String> requestData;
            for (Response<List<String>> response : storeRequestsResponses) {
                requestData = response.get();
                if (requestData != null && !requestData.isEmpty())
                    pendingRequests.add(parseAccessRequestData(requestData));
            }
            return pendingRequests;
        }
    }

    public static void deleteAccessRequest(String storeID, String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.lrem(String.format(STORE_PENDING_ACCESS_REQUESTS, storeID), 1, requestID);
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
            t.hset(key, TOKEN_STORE_FIELD, store);
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
                LocksClient.releaseStoreLock(
                        values.get(TOKEN_USER_FIELD),
                        values.get(TOKEN_STORE_FIELD),
                        lockID);
            jedis.del(String.format(ACCESS_TOKEN, tokenID));
        }
    }

    public static void init(String defaultUsername, String defaultPass) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, defaultUsername), defaultPass);
            t.sadd(String.format(USER_ROLE, defaultUsername), ADMIN.toString());
            t.sadd(STATUS, String.valueOf(true));
            t.exec();
        }
    }

    public static boolean isInit() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(STATUS);
        }
    }


}
