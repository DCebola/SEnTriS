package pt.fct.nova.id.srv.application.storage.redis;

import com.google.gson.Gson;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.storage.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.nio.ByteBuffer;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class RStorageEngine implements StorageEngine {

    final static Logger logger = LoggerFactory.getLogger(RStorageEngine.class);
    private final static String INFO = "%s:INFO";
    private final static String NAMESPACES = "%s:NS";

    private final static String S_IRIS = "%s:S:IRIS";
    private final static String P_IRIS = "%s:P:IRIS";
    private final static String O_IRIS = "%s:O:IRIS";

    private final static String REV_S_IRIS = "%s:S:REV_IRIS";
    private final static String REV_P_IRIS = "%s:P:REV_IRIS";
    private final static String REV_O_IRIS = "%s:O:REV_IRIS";

    private final static String ALL_S = "%s:S";
    private final static String ALL_P = "%s:P";
    private final static String ALL_O = "%s:O";
    private final static String ALL_SP = "%s:SP";
    private final static String ALL_SO = "%s:SO";
    private final static String ALL_PO = "%s:PO";

    private final static String SINGLE_S = "%s:S:%s";
    private final static String SINGLE_P = "%s:P:%s";
    private final static String SINGLE_O = "%s:O:%s";
    private final static String SINGLE_SP = "%s:SP:%s";
    private final static String SINGLE_SO = "%s:SO:%s";
    private final static String SINGLE_PO = "%s:PO:%s";
    private static final String COMPLEMENT_INDEX_SEPARATOR = ":";


    private final static String BLANK_IRI = "B::%s";
    private static final String BLANK_IRI_PREFIX = "B";
    private static final String SIMPLE_IRI = "S::%s";
    private static final String SIMPLE_IRI_PREFIX = "S";
    private static final String LITERAL_IRI = "L::%s::%s";
    private static final int IRI_PREFIX_POS = 0;
    private static final int IRI_VALUE_POS = 1;
    private static final int LITERAL_IRI_DATATYPE_POS = 2;
    private static final String IRI_SEPARATOR = "::";


    @Override
    public boolean setupStore(String storeID, Map<String, String> namespaces) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            t.set(String.format(INFO, storeID), String.valueOf(false));
            namespaces.forEach((k, v) -> putNamespace(t, storeID, k, v));
            t.exec();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void putNamespace(Transaction t, String storeID, String namespaceKey, String namespaceValue) {
        t.hset(String.format(NAMESPACES, storeID), namespaceKey, namespaceValue);
    }


    @Override
    public boolean deleteStore(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            jedis.set(String.format(INFO, storeID), String.valueOf(true));
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

            boolean isDeleted = Boolean.parseBoolean(jedis.get(storeInfo));

            if (!isDeleted) {

                Pipeline p = jedis.pipelined();
                Response<String> resp_s = getIndexFromIRI(p, S_IRIS, storeID, s_iri);
                Response<String> resp_p = getIndexFromIRI(p, P_IRIS, storeID, p_iri);
                Response<String> resp_o = getIndexFromIRI(p, O_IRIS, storeID, o_iri);
                p.sync();

                String s_idx = resp_s.get();
                String p_idx = resp_p.get();
                String o_idx = resp_o.get();

                Transaction t = jedis.multi();
                t.watch(storeInfo);


                if (s_idx == null) {
                    s_idx = generateID();
                    putIRI(t, S_IRIS, REV_S_IRIS, storeID, s_iri, s_idx);
                }
                if (p_idx == null) {
                    p_idx = generateID();
                    putIRI(t, P_IRIS, REV_P_IRIS, storeID, p_iri, p_idx);
                }
                if (o_idx == null) {
                    o_idx = generateID();
                    putIRI(t, O_IRIS, REV_O_IRIS, storeID, o_iri, o_idx);
                }

                logger.info("#{}: Indexes=[s_{}, p_{}, o_{}]", storeID, s_idx, p_idx, o_idx);

                String sp_idx = generateComplementIndex(p_idx, s_idx);
                String so_idx = generateComplementIndex(s_idx, o_idx);
                String po_idx = generateComplementIndex(p_idx, o_idx);

                putIndex(t, ALL_S, SINGLE_S, storeID, s_idx, po_idx);
                putIndex(t, ALL_P, SINGLE_P, storeID, p_idx, so_idx);
                putIndex(t, ALL_O, SINGLE_O, storeID, o_idx, sp_idx);

                putIndex(t, ALL_SP, SINGLE_SP, storeID, sp_idx, o_idx);
                putIndex(t, ALL_SO, SINGLE_SO, storeID, so_idx, p_idx);
                putIndex(t, ALL_PO, SINGLE_PO, storeID, po_idx, s_idx);

                logger.info("#{}: Pipelined INDEX uploads.", storeID);

                t.exec();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private String generateID() {
        return uuidToBase64(UUID.randomUUID().toString());
    }

    private static String uuidToBase64(String str) {
        UUID uuid = UUID.fromString(str);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return encodeBase64URLSafeString(bb.array());
    }

    private static String uuidFromBase64(String str) {
        byte[] bytes = decodeBase64(str);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();
    }

    private Response<String> getIndexFromIRI(Pipeline p, String keyFormatter, String storeID, String iri) {
        return p.hget(String.format(keyFormatter, storeID), iri);
    }

    private String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_IRI, node.getBlankNodeId());
    }

    private Node generateNode(String iri) {
        String[] split_iri = iri.split(IRI_SEPARATOR);
        logger.info("#Split {}", Arrays.toString(split_iri));
        if (split_iri[IRI_PREFIX_POS].equals(BLANK_IRI_PREFIX))
            return NodeFactory.createBlankNode(split_iri[IRI_VALUE_POS]);
        else if (split_iri[IRI_PREFIX_POS].equals(SIMPLE_IRI_PREFIX))
            return NodeFactory.createURI(split_iri[IRI_VALUE_POS]);
        else
            return NodeFactory.createLiteral(
                    split_iri[IRI_VALUE_POS],
                    TypeMapper.getInstance().getSafeTypeByName(split_iri[LITERAL_IRI_DATATYPE_POS]));
    }

    private void putIRI(Transaction transaction, String keyFormatter, String reverseKeyFormatter, String storeID, String nodeIRI, String idx) {
        transaction.hset(String.format(keyFormatter, storeID), nodeIRI, idx);
        transaction.hset(String.format(reverseKeyFormatter, storeID), idx, nodeIRI);
    }

    private void putIndex(Transaction transaction, String allKeyFormatter, String singleKeyFormatter, String storeID, String idx, String complementIdx) {
        transaction.sadd(String.format(allKeyFormatter, storeID), idx);
        transaction.sadd(String.format(singleKeyFormatter, storeID, idx), complementIdx);
    }

    private String generateComplementIndex(String idx1, String idx2) {
        return idx1.concat(COMPLEMENT_INDEX_SEPARATOR).concat(idx2);
    }

    @Override
    public List<Triple> getTriples(String storeID) {
        List<Triple> triples = new LinkedList<>();

        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> s_idxs = jedis.smembers(String.format(ALL_S, storeID));
            Set<String> po_idxs;
            String s_iri, p_iri, o_iri;
            String p_idx, o_idx;
            String[] po_split;
            for (String s_idx : s_idxs) {
                po_idxs = jedis.smembers(String.format(SINGLE_S, storeID, s_idx));
                if (po_idxs != null) {
                    for (String po_idx : po_idxs) {
                        po_split = po_idx.split(COMPLEMENT_INDEX_SEPARATOR);
                        p_idx = po_split[0];
                        o_idx = po_split[1];
                        s_iri = jedis.hget(String.format(REV_S_IRIS, storeID), s_idx);
                        p_iri = jedis.hget(String.format(REV_P_IRIS, storeID), p_idx);
                        o_iri = jedis.hget(String.format(REV_O_IRIS, storeID), o_idx);

                        logger.info("#{}: po->{}", storeID, po_idx);
                        logger.info("#{}: ({}) -> [{}] -> ({})", storeID, s_iri, p_iri, o_iri);
                        logger.info("#{}: Indexes=[s_{}, p_{}, o_{}]", storeID, s_idx, p_idx, o_idx);

                        triples.add(new Triple(
                                generateNode(s_iri),
                                generateNode(p_iri),
                                generateNode(o_iri)));
                    }
                }
            }
            return triples;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, String> getNamespaces(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(String.format(NAMESPACES, storeID));
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private Set<Node> getNodes(String storeID, Set<String> idxs) {
        return null;
    }

    private Node getNode(String storeID, String idx) {
        return null;
    }

    private Set<UUID> getS(String storeID) {
        return null;
    }

    private Set<UUID> getP(String storeID) {
        return null;
    }

    private Set<UUID> getO(String storeID) {
        return null;
    }

    private Set<UUID> getSP(String storeID) {
        return null;
    }

    private Set<UUID> getSO(String storeID) {
        return null;
    }

    private Set<UUID> getPO(String storeID) {
        return null;
    }

    private Set<UUID> findByIRI(String storeID, String nodeIRI) {
        return null;
    }

    private Set<UUID> findByP(String storeID, String cpIdx) {
        return null;
    }

    private Set<UUID> findByS(String storeID, String cpIdx) {
        return null;
    }

    private Set<UUID> findByO(String storeID, String cpIdx) {
        return null;
    }

    private Set<UUID> findBySP(String storeID, String cpIdx) {
        return null;
    }

    private Set<UUID> findBySO(String storeID, String cpIdx) {
        return null;
    }

    private Set<UUID> findByPO(String storeID, String cpIdx) {
        return null;
    }


}
