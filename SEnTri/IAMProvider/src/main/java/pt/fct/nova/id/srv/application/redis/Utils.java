package pt.fct.nova.id.srv.application.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class Utils {

    public static Set<String> scan(Jedis jedis, String scanPattern){
        String cursor = SCAN_POINTER_START;
        ScanParams params = new ScanParams();
        params.match(scanPattern);
        Set<String> res = new HashSet<>();
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, params);
            res.addAll(scanResult.getResult());
            cursor = scanResult.getCursor();
        } while (!cursor.equals(SCAN_POINTER_START));
        return res;
    }
}
