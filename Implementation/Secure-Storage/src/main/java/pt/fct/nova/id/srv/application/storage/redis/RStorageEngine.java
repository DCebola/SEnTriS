package pt.fct.nova.id.srv.application.storage.redis;

import com.google.gson.Gson;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    final static Logger logger = LoggerFactory.getLogger(RStorageEngine.class);
    private final static String INFO = "%s:INFO";
    private final static String TRIPLE_COUNT = "T_COUNT";
    private final static String NODES = "%s:NODES";
    private final static String IRIS = "%s:IRIS";
    private final static String IDX_TABLE_S = "%s:S";
    private final static String IDX_TABLE_P = "%s:P";
    private final static String IDX_TABLE_O = "%s:O";
    private final static String IDX_TABLE_SP = "%s:SP";
    private final static String IDX_TABLE_SO = "%s:SO";
    private final static String IDX_TABLE_PO = "%s:PO";

    private final static String BLANK_IRI = "_";

    @Override
    public boolean setupStore(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.hset(String.format(INFO, storeID), TRIPLE_COUNT, String.valueOf(0));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public boolean deleteStore(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.del(
                    String.format(INFO, storeID),
                    String.format(NODES, storeID),
                    String.format(IRIS, storeID),
                    String.format(IDX_TABLE_S, storeID),
                    String.format(IDX_TABLE_P, storeID),
                    String.format(IDX_TABLE_O, storeID),
                    String.format(IDX_TABLE_SP, storeID),
                    String.format(IDX_TABLE_SO, storeID),
                    String.format(IDX_TABLE_PO, storeID)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public boolean saveTriple(String storeID, Triple triple) {

        String storeInfo = String.format(INFO, storeID);
        Node subject = triple.getSubject();
        Node predicate = triple.getPredicate();
        Node object = triple.getObject();
        logger.info("#{}: Triple:{}", storeID, triple);

        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String s_iri = parseNodeIRI(subject);
            String p_iri = parseNodeIRI(predicate);
            String o_iri = parseNodeIRI(object);

            logger.info("#{}: ({}) -> [{}] -> ({})", storeID, s_iri, p_iri, o_iri);

            String tCountRes = jedis.hget(storeInfo, TRIPLE_COUNT);

            if (tCountRes != null) {
                int tCount = Integer.parseInt(tCountRes);
                logger.info("#{}: triples={}", storeID, tCount);

                Transaction t = jedis.multi();
                t.watch(storeInfo);

                Index s_idx = IndexFactory.createIndex(IndexType.S, tCount);
                Index p_idx = IndexFactory.createIndex(IndexType.P, tCount);
                Index o_idx = IndexFactory.createIndex(IndexType.O, tCount);
                Index sp_idx = IndexFactory.createCompoundIndex(s_idx, p_idx);
                Index so_idx = IndexFactory.createCompoundIndex(s_idx, o_idx);
                Index po_idx = IndexFactory.createCompoundIndex(p_idx, o_idx);

                String s_idx_parsed = serializeIndex(s_idx);
                String p_idx_parsed = serializeIndex(p_idx);
                String o_idx_parsed = serializeIndex(o_idx);
                String sp_idx_parsed = serializeIndex(sp_idx);
                String so_idx_parsed = serializeIndex(so_idx);
                String po_idx_parsed = serializeIndex(po_idx);

                logger.info("#{}: Indexes=[s_{}, p_{}, o_{}, sp_{}, so_{}, po_{}]", storeID,
                        s_idx_parsed, p_idx_parsed, o_idx_parsed, sp_idx_parsed, so_idx_parsed, po_idx_parsed);

                putNode(t, storeID, s_idx_parsed, serializeNode(subject));
                putNode(t, storeID, p_idx_parsed, serializeNode(predicate));
                putNode(t, storeID, o_idx_parsed, serializeNode(object));

                logger.info("#{}: Pipelined node uploads.", storeID);

                putIRI(t, storeID, s_iri, s_idx_parsed);
                putIRI(t, storeID, p_iri, p_idx_parsed);
                putIRI(t, storeID, o_iri, o_idx_parsed);

                logger.info("#{}: Pipelined IRI uploads.", storeID);

                putIndex(t, storeID, IDX_TABLE_S, s_idx_parsed, po_idx_parsed);
                putIndex(t, storeID, IDX_TABLE_P, p_idx_parsed, so_idx_parsed);
                putIndex(t, storeID, IDX_TABLE_O, o_idx_parsed, sp_idx_parsed);
                putIndex(t, storeID, IDX_TABLE_SP, sp_idx_parsed, o_idx_parsed);
                putIndex(t, storeID, IDX_TABLE_SO, so_idx_parsed, p_idx_parsed);
                putIndex(t, storeID, IDX_TABLE_PO, po_idx_parsed, s_idx_parsed);

                logger.info("#{}: Pipelined INDEX uploads.", storeID);

                t.hset(storeInfo, TRIPLE_COUNT, String.valueOf(tCount + 1));

                logger.info("#{}: Pipelined TRIPLE_COUNT increment.", storeID);

                t.exec();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String serializeIndex(Index idx) {
        return gson.toJson(idx);
    }

    private String serializeNode(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isLiteral()){
            logger.info("Literal: {} {} {}", node.getLiteral().getLexicalForm(), node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
            return node.getLiteralLexicalForm();
        }
        return gson.toJson(node);
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
