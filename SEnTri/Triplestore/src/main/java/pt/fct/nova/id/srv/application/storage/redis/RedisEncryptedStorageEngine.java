package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.Utils;
import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class RedisEncryptedStorageEngine implements EncryptedStorageEngine {
    public static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    public final static String KEY_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s");
    public static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");
    public static final long COMMIT_LIFETIME = Long.parseLong(System.getenv("COMMIT_LIFETIME"));
    private final static byte[] UPDATE_SCRIPT = """
            for i = 1, #ARGV, 1 do
                for _, key in ipairs(redis.call('smembers', ARGV[i])) do
                    redis.call('del', key)
                end
            end
            for i = 1, #KEYS, 1 do
                local key
            	for j, value in ipairs(redis.call('hgetall', KEYS[i])) do
            		if j % 2 == 1 then
            			key = value
            		else
            		    redis.call('set', key, value)
            		end
            	end
            end
            return 0
            """.getBytes(StandardCharsets.UTF_8);

    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            Redis.scan(jedis, String.format(TRIPLESTORE_DATA_PATTERN, triplestoreID).getBytes(StandardCharsets.UTF_8)).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public byte[] commitUpload(String triplestoreID, Map<byte[], byte[]> encryptedNodes) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            byte[] id = Utils.generateID();
            jedis.hset(id, encryptedNodes);
            jedis.expire(id, COMMIT_LIFETIME);
            return id;
        }
    }

    @Override
    public void update(String triplestoreID, List<byte[]> uploads, List<byte[]> deletions) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            List<byte[]> keys = new ArrayList<>(uploads.size());
            List<byte[]> args = new ArrayList<>(deletions.size());
            keys.addAll(uploads);
            args.addAll(deletions);
            System.out.println(jedis.eval(UPDATE_SCRIPT, keys, args));
        }
    }

    @Override
    public List<byte[]> search(String triplestoreID, List<byte[]> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<byte[]>> responses = new ArrayList<>(trapdoors.size());
            trapdoors.forEach(trapdoor ->
                    responses.add(p.get(String.format(KEY_FORMAT, triplestoreID, new String(trapdoor, StandardCharsets.UTF_8))
                            .getBytes(StandardCharsets.UTF_8))));

            p.sync();
            List<byte[]> res = new ArrayList<>(trapdoors.size());
            int total = 0;
            for (Response<byte[]> r : responses) {
                res.add(r.get());
                total++;
            }

            System.out.println("SEARCH: " + trapdoors.size() + " | " + "FOUND: " + total);
            return res;
        }
    }
}
