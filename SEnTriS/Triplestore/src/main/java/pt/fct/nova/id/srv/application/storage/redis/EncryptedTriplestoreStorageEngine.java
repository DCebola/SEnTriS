package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.Utils;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class EncryptedTriplestoreStorageEngine implements EncryptedStorageEngine {

    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    public static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");
    public static final long COMMIT_LIFETIME = Long.parseLong(System.getenv("COMMIT_LIFETIME"));

    public final static String UPDATE_SCRIPT = """
            local numDeletions = tonumber(ARGV[1])
            local len
            local total
            for i, key in ipairs(KEYS) do
                if i <= numDeletions then
                    len = redis.call("llen", key)
                    local t
                    total = 0
                    for i = 1, len, 1 do
                        t = redis.call("lpop", key)
                        redis.call("del", t)
                        total = total + 1
                    end
                else
                    len = redis.call("llen", key)
                    local t
                    local n
                    total = 0
                    for i = 1, len, 1 do
                        if (i % 2 == 1) then
                            t = redis.call("lpop", key)
                        else
                            n = redis.call("lpop", key)
                            redis.call("set", t, n)
                            total = total + 2
                        end
                    end
                end
                redis.call("del", key)
            end
            """;

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            TriplestoreStorageEngine.deleteAll(triplestoreID, jedis, TRIPLESTORE_DATA_PATTERN);
        }
    }

    @Override
    public String commitUpload(String triplestoreID, Map<String, String> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String id = Utils.generateID();
            encryptedNodes.forEach((k, v) -> t.lpush(id, v, String.format(KEY_FORMAT, triplestoreID, k)));
            System.out.println("Uploads:" + encryptedNodes.entrySet().size() + " | " + id);
            t.expire(id, COMMIT_LIFETIME);
            t.exec();
            return id;
        }
    }

    @Override
    public String commitDelete(String triplestoreID, Set<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String id = Utils.generateID();
            trapdoors.forEach(trapdoor -> t.lpush(id, String.format(KEY_FORMAT, triplestoreID, trapdoor, StandardCharsets.UTF_8)));
            System.out.println("Deletions:" + trapdoors.size() + " | " + id);
            t.expire(id, COMMIT_LIFETIME);
            t.exec();
            return id;
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
            String data;
            for (Response<String> r : responses) {
                data = r.get();
                if (data == null)
                    res.add(null);
                else
                    res.add(base64Decoder.decode(r.get()));
                total++;
            }

            System.out.println("SEARCH: " + trapdoors.size() + " | " + "FOUND: " + total);
            return res;
        }
    }

    @Override
    public void update(List<String> uploads, List<String> deletions) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<String> keys = new ArrayList<>(uploads.size() + deletions.size());
            List<String> args = new ArrayList<>(1);
            System.out.println("Deletion IDs: " + Arrays.toString(deletions.toArray()) + " | " + deletions.size());
            System.out.println("Uploads IDs: " + Arrays.toString(uploads.toArray()) + " | " + uploads.size());
            keys.addAll(deletions);
            keys.addAll(uploads);
            args.add(String.valueOf(deletions.size()));
            jedis.eval(UPDATE_SCRIPT, keys, args);
        }
    }
}
