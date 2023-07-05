package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.Utils;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class RedisEncryptedStorageEngine implements EncryptedStorageEngine {

    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    public static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");
    public static final long COMMIT_LIFETIME = Long.parseLong(System.getenv("COMMIT_LIFETIME"));
    private final static String UPDATE_SCRIPT = """
            local function batchUpload(key)
                local len = redis.call("llen", key)
                local trapdoor
                local node
                print("Upload: ", key)
                for i = 0, len, 2 do
                    trapdoor =  redis.call("lpop", key)[1]
                    print("(t" .. i  .. ": " .. trapdoor)
                    node = redis.call("lpop", key)[1]
                    print("(n" .. i .. ": " .. trapdoor)
                    redis.call("set", trapdoor, node);
                end
                redis.call("del", key)
                return len / 2
            end
                        
            local function batchDelete(key)
                local len = redis.call("llen", key)
                local trapdoor
                print("Delete: ", key)
                for i = 0, len, 1 do
                    trapdoor = redis.call("lpop", key)[1]
                    print("(t" .. i  .. ": " .. trapdoor)
                    redis.call("del", trapdoor);
                end
                redis.call("del", key)
                return len
            end
                        
            local uploads = 0
            local deletions = 0
            for i = 0, ARGV[1], 1 do
                deletions = deletions + batchDelete(KEYS[i + 1])
            end
            for i = ARGV[1], ARGV[2], 1 do
                uploads = uploads + batchUpload(KEYS[i + 1])
            end
            print("Uploads: " .. uploads .. "| Deletions: " .. deletions)
            """;

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            Redis.scan(jedis, String.format(TRIPLESTORE_DATA_PATTERN, triplestoreID).getBytes(StandardCharsets.UTF_8)).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public String commitUpload(String triplestoreID, Map<String, String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String id = Utils.generateID();
            encryptedNodes.forEach((k, v) -> jedis.lpush(id, String.format(KEY_FORMAT, triplestoreID, k), v));
            jedis.expire(id, COMMIT_LIFETIME);
            return id;
        }
    }

    @Override
    public void update(String triplestoreID, List<String> uploads, List<String> deletions) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(uploads.size() + deletions.size());
            List<String> args = new ArrayList<>(2);
            keys.addAll(uploads);
            keys.addAll(deletions);
            args.add(String.valueOf(uploads.size()));
            args.add(String.valueOf(deletions.size()));
            jedis.eval(UPDATE_SCRIPT, keys, args);
        }
    }

    @Override
    public List<byte[]> search(String triplestoreID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(trapdoor -> responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, trapdoor))));
            p.sync();
            List<byte[]> res = new ArrayList<>(trapdoors.size());
            int total = 0;
            for (Response<String> r : responses) {
                res.add(base64Decoder.decode(r.get()));
                total++;
            }

            System.out.println("SEARCH: " + trapdoors.size() + " | " + "FOUND: " + total);
            return res;
        }
    }
}
