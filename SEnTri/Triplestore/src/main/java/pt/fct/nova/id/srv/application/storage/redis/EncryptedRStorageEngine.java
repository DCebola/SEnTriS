package pt.fct.nova.id.srv.application.storage.redis;

import pt.fct.nova.id.srv.application.storage.EncryptedStorageEngine;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.exceptions.StorageEngineException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreAlreadyExistsException;
import pt.fct.nova.id.srv.application.storage.exceptions.StoreNotFoundException;
import pt.fct.nova.id.srv.application.storage.iri_tables.IRITable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class EncryptedRStorageEngine implements EncryptedStorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private final static String STORE_STATE = "%s".concat(BASIC_SEPARATOR).concat("STATE");
    private static final String STORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void setupStore(String storeID) throws StorageEngineException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(STORE_STATE, storeID), String.valueOf(false));
            t.exec();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void deleteStore(String storeID) throws StorageEngineException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(STORE_STATE, storeID), String.valueOf(true));
            deleteStoreData(jedis, storeID);
        } catch (Exception e) {
            throw new StorageEngineException();
        }
    }

    private void deleteStoreData(Jedis jedis, String storeID) {
        ScanParams params = new ScanParams();
        params.match(String.format(STORE_DATA_PATTERN, storeID));
        String cursor = SCAN_POINTER_START;
        Set<String> collector = new HashSet<>();
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, params);
            List<String> res = scanResult.getResult();
            collector.addAll(res);
            cursor = scanResult.getCursor();
        } while (!cursor.equals(SCAN_POINTER_START));
        Transaction t = jedis.multi();
        t.del(collector.toArray(String[]::new));
        t.del(String.format(STORE_STATE, storeID));
        t.exec();
    }

    @Override
    public void save(String storeID, Map<String, String> encryptedNodes) throws StorageEngineException {
        String storeState = String.format(STORE_STATE, storeID);
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            boolean isDeleted = Boolean.parseBoolean(jedis.get(storeState));
            if (!isDeleted) {
                Transaction t = jedis.multi();
                t.watch(storeState);
                encryptedNodes.forEach(t::set);
                t.exec();
            } else
                deleteStore(storeID);
        } catch (Exception e) {
            throw new StorageEngineException();
        }
    }

    @Override
    public void checkID(String storeID) throws StoreAlreadyExistsException, StoreNotFoundException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            if (jedis.get(String.format(STORE_STATE, storeID)) != null)
                throw new StoreAlreadyExistsException();
            else
                throw new StoreNotFoundException();
        } catch (StoreAlreadyExistsException | StoreNotFoundException e) {
            throw e;
        } catch (Exception ignored) {
        }

    }

    @Override
    public IRITable search(String storeID, List<String> trapdoors) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            List<Response<String>> responses;
            trapdoors.forEach(p::get);
            p.sync();

        } catch (Exception e) {
            throw new StorageEngineException();
        }
    }
}
