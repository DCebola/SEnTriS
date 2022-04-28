package pt.fct.nova.id.srv.application.storage.redis;

import com.google.gson.Gson;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.indexes.Index;
import pt.fct.nova.id.srv.application.indexes.IndexFactory;
import pt.fct.nova.id.srv.application.indexes.IndexType;
import pt.fct.nova.id.srv.application.storage.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Iterator;
import java.util.Set;

public class RStorageEngine implements StorageEngine {

    private final Gson gson = new Gson();
    private final static String INFO = "%S:INFO";
    private final static String TRIPLE_COUNT = "T_COUNT";
    private final static String NODES = "%S:NODES";
    private final static String IRIS = "%S:IRIS";
    private final static String IDX_TABLE_S = "%S:S";
    private final static String IDX_TABLE_P = "%S:P";
    private final static String IDX_TABLE_O = "%S:O";
    private final static String IDX_TABLE_SP = "%S:SP";
    private final static String IDX_TABLE_SO = "%S:SO";
    private final static String IDX_TABLE_PO = "%S:PO";

    private final static String BLANK_IRI = "_";

    @Override
    public synchronized boolean setupStore(String storeID) {
        return false;
    }


    @Override
    public synchronized boolean deleteStore(String storeID) {
        return false;
    }


    @Override
    public synchronized boolean saveTriple(String storeID, Triple triple) {
        String storeInfo = String.format(INFO, storeID);
        Node subject = triple.getSubject();
        Node predicate = triple.getObject();
        Node object = triple.getObject();

        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String s_iri = parseNodeIRI(subject);
            String p_iri = parseNodeIRI(predicate);
            String o_iri = parseNodeIRI(object);

            int tCount = Integer.parseInt(jedis.hget(storeInfo, TRIPLE_COUNT));

            Transaction t = jedis.multi();
            t.watch(storeInfo);

            Index s_idx = IndexFactory.createIndex(IndexType.S, tCount);
            Index p_idx = IndexFactory.createIndex(IndexType.S, tCount);
            Index o_idx = IndexFactory.createIndex(IndexType.S, tCount);
            Index sp_idx = IndexFactory.createCompoundIndex(s_idx, p_idx);
            Index so_idx = IndexFactory.createCompoundIndex(s_idx, o_idx);
            Index po_idx = IndexFactory.createCompoundIndex(p_idx, o_idx);

            String s_idx_parsed = gson.toJson(s_idx);
            String p_idx_parsed = gson.toJson(p_idx);
            String o_idx_parsed = gson.toJson(o_idx);
            String sp_idx_parsed = gson.toJson(sp_idx);
            String so_idx_parsed = gson.toJson(so_idx);
            String po_idx_parsed = gson.toJson(po_idx);

            putNode(t, storeID, s_idx_parsed, gson.toJson(subject));
            putNode(t, storeID, p_idx_parsed, gson.toJson(predicate));
            putNode(t, storeID, o_idx_parsed, gson.toJson(object));

            putIRI(t, storeID, s_iri, s_idx_parsed);
            putIRI(t, storeID, p_iri, p_idx_parsed);
            putIRI(t, storeID, o_iri, o_idx_parsed);

            putIndex(t, storeID, IDX_TABLE_S, s_idx_parsed, po_idx_parsed);
            putIndex(t, storeID, IDX_TABLE_P, p_idx_parsed, so_idx_parsed);
            putIndex(t, storeID, IDX_TABLE_O, o_idx_parsed, sp_idx_parsed);
            putIndex(t, storeID, IDX_TABLE_SP, sp_idx_parsed, o_idx_parsed);
            putIndex(t, storeID, IDX_TABLE_SO, so_idx_parsed, p_idx_parsed);
            putIndex(t, storeID, IDX_TABLE_PO, po_idx_parsed, s_idx_parsed);

            t.hset(storeInfo, TRIPLE_COUNT, String.valueOf(tCount + 1));

            t.exec();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return node.getURI();
        else if (node.isLiteral())
            return node.getLiteral().getLexicalForm();
        else
            return BLANK_IRI;
    }

    private void putNode(Transaction transaction, String storeID, String idx, String node) {
        transaction.hset(String.format(NODES, storeID), idx, node);
    }

    private void putIRI(Transaction transaction, String storeID, String nodeIRI, String idx) {
        transaction.hset(String.format(IRIS, storeID), nodeIRI, idx);
    }

    private void putIndex(Transaction transaction, String storeID, String indexID, String idx, String complementaryIdx) {
        transaction.hset(String.format(indexID, storeID), idx, complementaryIdx);
    }

    @Override
    public Iterator<Triple> getTriples(String storeID) {
        return null;
    }

    private Set<Node> getNodes(String storeID, Set<String> idxs) {
        return null;
    }

    private Node getNode(String storeID, String idx) {
        return null;
    }

    private Set<Index> getS(String storeID) {
        return null;
    }

    private Set<Index> getP(String storeID) {
        return null;
    }

    private Set<Index> getO(String storeID) {
        return null;
    }

    private Set<Index> getSP(String storeID) {
        return null;
    }

    private Set<Index> getSO(String storeID) {
        return null;
    }

    private Set<Index> getPO(String storeID) {
        return null;
    }

    private Set<Index> findByIRI(String storeID, String nodeIRI) {
        return null;
    }

    private Set<Index> findByP(String storeID, String cpIdx) {
        return null;
    }

    private Set<Index> findByS(String storeID, String cpIdx) {
        return null;
    }

    private Set<Index> findByO(String storeID, String cpIdx) {
        return null;
    }

    private Set<Index> findBySP(String storeID, String cpIdx) {
        return null;
    }

    private Set<Index> findBySO(String storeID, String cpIdx) {
        return null;
    }

    private Set<Index> findByPO(String storeID, String cpIdx) {
        return null;
    }


}
