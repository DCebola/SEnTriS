package pt.fct.nova.id.srv.application.storage.redis;

import com.google.gson.Gson;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.indexes.Index;
import pt.fct.nova.id.srv.application.storage.StorageEngine;

import java.util.Iterator;
import java.util.Set;

public class RStorageEngine implements StorageEngine {

    private final Gson gson = new Gson();

    /*
    public static void put(String key, String value) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map get(String key) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return gson.fromJson(jedis.get(key), Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    @Override
    public Set<Index> findByIRI(String storeID, String nodeIRI) {
        return null;
    }

    @Override
    public Set<Node> getNodes(String storeID, Set<Index> idxs) {
        return null;
    }

    @Override
    public Iterator<Triple> getTriples(String storeID) {
        return null;
    }

    @Override
    public Node getNode(String storeID, Index idx) {
        return null;
    }

    @Override
    public Set<Index> findByP(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> findByS(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> findByO(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> findBySP(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> findBySO(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> findByPO(String storeID, Index cpIdx) {
        return null;
    }

    @Override
    public Set<Index> getS(String storeID) {
        return null;
    }

    @Override
    public Set<Index> getP(String storeID) {
        return null;
    }

    @Override
    public Set<Index> getO(String storeID) {
        return null;
    }

    @Override
    public Set<Index> getSP(String storeID) {
        return null;
    }

    @Override
    public Set<Index> getSO(String storeID) {
        return null;
    }

    @Override
    public Set<Index> getPO(String storeID) {
        return null;
    }

    @Override
    public Index putNode(String storeID, Node node) {
        return null;
    }

    @Override
    public boolean putIRI(String storeID, String nodeIRI, Index idx) {
        return false;
    }

    @Override
    public boolean putS(String storeID, Index idx) {
        return false;
    }

    @Override
    public boolean putP(String storeID, Index idx) {
        return false;
    }

    @Override
    public boolean putO(String storeID, Index idx) {
        return false;
    }

    @Override
    public boolean putSP(String storeID, Index idx) {
        return false;
    }

    @Override
    public boolean putSO(String storeID, Index idx) {
        return false;
    }

    @Override
    public boolean putPO(String storeID, Index idx) {
        return false;
    }

    @Override
    public void deleteStore(String storeID) {

    }
}
