package pt.fct.nova.id.srv.application.clients.iam;

import com.google.gson.Gson;
import jakarta.ws.rs.core.NewCookie;
import pt.fct.nova.id.srv.application.clients.redis.Redis;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;

import static pt.fct.nova.id.srv.presentation.api.dtos.Role.ADMIN;

public class IAMStore {
    private static final Gson gson = new Gson();
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String COOKIE_PARAM = "session";
    private static final int COOKIE_LIFETIME = Integer.parseInt(System.getenv("COOKIE_LIFETIME"));
    private static final String SESSION = "S".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_PASSWORD = "UP".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_ROLE = "UR".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_READ_ACCESS = "SRA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_WRITE_ACCESS = "SWA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_OWNER = "SO".concat(BASIC_SEPARATOR).concat("%s");

    private static final String PENDING_ACCESS_REQUESTS = "PA";
    private static final String PENDING_ROLE_REQUESTS = "PR";
    private static final String STORE_STATE = "STORE_STATE";

    public static NewCookie cacheSession(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String key = String.format(SESSION, username);
            String uuid = UUID.randomUUID().toString();
            Transaction t = jedis.multi();
            t.del(key);
            t.set(key, uuid);
            t.expire(key, Integer.toUnsignedLong(COOKIE_LIFETIME));
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

    public static void deleteUser(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(USER_PASSWORD, username));
            t.del(String.format(USER_ROLE, username));
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

    public static boolean checkIfOwns(String username, String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(STORE_OWNER, storeID)).equals(username);
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
            return r1.get() && r2.get();
        }
    }

    public static void saveStoreAccessPolicy(String storeID, String owner, Set<String> read, Set<String> write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(STORE_OWNER, storeID), owner);
            for (String username : read)
                t.sadd(String.format(STORE_READ_ACCESS, storeID), username);
            for (String username : write)
                t.sadd(String.format(STORE_WRITE_ACCESS, storeID), username);
            t.exec();
        }
    }

    public static void deleteStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(STORE_OWNER, storeID));
            t.del(String.format(STORE_READ_ACCESS, storeID));
            t.del(String.format(STORE_WRITE_ACCESS, storeID));
            t.exec();
        }
    }

    public static void revokeAccess(String storeID, String username, boolean read, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (read)
                t.srem(String.format(STORE_READ_ACCESS, username), storeID);
            if (write)
                t.srem(String.format(STORE_WRITE_ACCESS, username), storeID);
            t.exec();
        }
    }

    public static void grantAccess(String storeID, String username, boolean read, boolean write) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (read)
                t.sadd(String.format(STORE_READ_ACCESS, username), storeID);
            if (write)
                t.sadd(String.format(STORE_WRITE_ACCESS, username), storeID);
            t.exec();
        }
    }

    public static void saveAccessRequest(AccessRequest accessRequest) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(PENDING_ACCESS_REQUESTS, UUID.randomUUID().toString(), gson.toJson(accessRequest, AccessRequest.class));
        }
    }

    public static void saveRoleRequest(RoleRequest roleRequest) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(PENDING_ROLE_REQUESTS, UUID.randomUUID().toString(), gson.toJson(roleRequest, RoleRequest.class));
        }
    }

    public static AccessRequest getPendingAccessRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String res = jedis.hget(String.format(PENDING_ACCESS_REQUESTS), requestID);
            if (res == null)
                return null;
            else return gson.fromJson(res, AccessRequest.class);
        }
    }

    public static RoleRequest getPendingRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String res = jedis.hget(String.format(PENDING_ROLE_REQUESTS), requestID);
            if (res == null)
                return null;
            else return gson.fromJson(res, RoleRequest.class);
        }
    }

    public static Set<String> getPendingAccessRequests() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hkeys(String.format(PENDING_ACCESS_REQUESTS));
        }
    }

    public static Set<String> getPendingRoleRequests() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hkeys(String.format(PENDING_ROLE_REQUESTS));
        }
    }

    public static void deleteAccessRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hdel(PENDING_ACCESS_REQUESTS, requestID);
        }
    }

    public static void deleteRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hdel(PENDING_ROLE_REQUESTS, requestID);
        }
    }

    public static void init(String defaultUsername, String defaultPass) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, defaultUsername), defaultPass);
            t.sadd(String.format(USER_ROLE, defaultUsername), ADMIN.toString());
            t.sadd(STORE_STATE, String.valueOf(true));
            t.exec();
        }
    }

    public static boolean isInit() {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.exists(STORE_STATE);
        }
    }
}
