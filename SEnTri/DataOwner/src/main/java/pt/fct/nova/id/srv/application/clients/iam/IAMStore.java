package pt.fct.nova.id.srv.application.clients.iam;

import com.google.gson.Gson;
import jakarta.ws.rs.core.NewCookie;
import pt.fct.nova.id.srv.application.clients.redis.Redis;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RoleForm;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

public class IAMStore {
    private static final Gson gson = new Gson();
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String COOKIE_PARAM = "session";
    private static final int COOKIE_LIFETIME = Integer.parseInt(System.getenv("COOKIE_LIFETIME"));
    private static final String SESSION = "S".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_PASSWORD = "UP".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_DATA = "UD".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_ACCESS_POLICY = "SA".concat(BASIC_SEPARATOR).concat("%s");
    private static final String PENDING_ACCESS_REQUESTS = "PA";
    private static final String PENDING_ROLE_REQUESTS = "PR";

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


    public static UserData getUserData(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(String.format(USER_DATA, username)), UserData.class);
        }
    }

    public static void saveUser(String username, UserData user) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(USER_DATA, username), gson.toJson(user, UserData.class));
        }
    }

    public static void saveUser(String username, String password, UserData user) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, username), password);
            t.set(String.format(USER_DATA, username), gson.toJson(user, UserData.class));
            t.exec();
        }
    }

    public static String getPassword(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(USER_PASSWORD, username));
        }
    }

    public static void deleteUser(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(USER_PASSWORD, username));
            t.del(String.format(USER_DATA, username));
            t.exec();
        }
    }

    public static StoreAccessPolicy getStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(String.format(STORE_ACCESS_POLICY, storeID)), StoreAccessPolicy.class);
        }
    }

    public static void saveStoreAccessPolicy(String storeID, StoreAccessPolicy storeAccessPolicy) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(STORE_ACCESS_POLICY, storeID), gson.toJson(storeAccessPolicy, StoreAccessPolicy.class));
        }
    }

    public static void deleteStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(String.format(STORE_ACCESS_POLICY, storeID));
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
            return gson.fromJson(jedis.hget(String.format(PENDING_ACCESS_REQUESTS), requestID), AccessRequest.class);
        }
    }

    public static RoleRequest getPendingRoleRequest(String requestID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.hget(String.format(PENDING_ROLE_REQUESTS), requestID), RoleRequest.class);
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
}
