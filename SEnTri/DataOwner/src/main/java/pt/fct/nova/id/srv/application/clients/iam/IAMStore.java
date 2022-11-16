package pt.fct.nova.id.srv.application.clients.iam;

import com.google.gson.Gson;
import jakarta.ws.rs.core.NewCookie;
import pt.fct.nova.id.srv.application.clients.redis.Redis;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RoleForm;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

public class IAMStore {
    private static final Gson gson = new Gson();
    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public static final String COOKIE_PARAM = "session";
    private static final int COOKIE_LIFETIME = Integer.parseInt(System.getenv("COOKIE_LIFETIME"));
    private static final String SESSION = "S".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_PASSWORD = "UP".concat(BASIC_SEPARATOR).concat("%s");
    private static final String USER_DATA = "UD".concat(BASIC_SEPARATOR).concat("%s");
    private static final String STORE_ACCESS_POLICY = "SA".concat(BASIC_SEPARATOR).concat("%s");

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static UserData getUserData(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(String.format(USER_DATA, username)), UserData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveUser(String username, UserData user) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(USER_DATA, username), gson.toJson(user, UserData.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveUser(String username, String password, UserData user) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(USER_PASSWORD, username), password);
            t.set(String.format(USER_DATA, username), gson.toJson(user, UserData.class));
            t.exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPassword(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.get(String.format(USER_PASSWORD, username));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void deleteUser(String username) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.del(String.format(USER_PASSWORD, username));
            t.del(String.format(USER_DATA, username));
            t.exec();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StoreAccessPolicy getStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(String.format(STORE_ACCESS_POLICY, storeID)), StoreAccessPolicy.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveStoreAccessPolicy(String storeID, StoreAccessPolicy storeAccessPolicy) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(STORE_ACCESS_POLICY, storeID), gson.toJson(storeAccessPolicy, StoreAccessPolicy.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteStoreAccessPolicy(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(String.format(STORE_ACCESS_POLICY, storeID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void enqueueAccessRequest(AccessPolicyForm accessPolicyForm) {
    }

    public static void enqueueRoleRequest(RoleForm roleForm) {
    }

    public static List<AccessPolicyForm> getPendingAccessRequests() {
        return null;
    }

    public static List<RoleForm> getPendingRoleRequests() {
        return null;
    }
}
