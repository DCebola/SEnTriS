package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.dao.TypedNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class RStorageEngine implements StorageEngine {

    final static Logger logger = LoggerFactory.getLogger(RStorageEngine.class);

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private static final String COMPOUND_INDEX_SEPARATOR = System.getenv("COMPOUND_INDEX_SEPARATOR");
    private static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");

    private final static String INFO = "%s".concat(BASIC_SEPARATOR).concat("INFO");
    private final static String NAMESPACES = "%s".concat(BASIC_SEPARATOR).concat("NS");

    private final static String S_IRIS = "%s".concat(BASIC_SEPARATOR).concat("S").concat(BASIC_SEPARATOR).concat("IRIS");
    private final static String P_IRIS = "%s".concat(BASIC_SEPARATOR).concat("P").concat(BASIC_SEPARATOR).concat("IRIS");
    private final static String O_IRIS = "%s".concat(BASIC_SEPARATOR).concat("O").concat(BASIC_SEPARATOR).concat("IRIS");

    private final static String REV_S_IRIS = "%s".concat(BASIC_SEPARATOR).concat("S").concat(BASIC_SEPARATOR).concat("REV_IRIS");
    private final static String REV_P_IRIS = "%s".concat(BASIC_SEPARATOR).concat("P").concat(BASIC_SEPARATOR).concat("REV_IRIS");
    private final static String REV_O_IRIS = "%s".concat(BASIC_SEPARATOR).concat("O").concat(BASIC_SEPARATOR).concat("REV_IRIS");

    private final static String ALL_S = "%s".concat(BASIC_SEPARATOR).concat("S");
    private final static String ALL_P = "%s".concat(BASIC_SEPARATOR).concat("P");
    private final static String ALL_O = "%s".concat(BASIC_SEPARATOR).concat("O");

    private final static String SINGLE_S = "%s".concat(BASIC_SEPARATOR).concat("S").concat(BASIC_SEPARATOR).concat("%s");
    private final static String SINGLE_P = "%s".concat(BASIC_SEPARATOR).concat("P").concat(BASIC_SEPARATOR).concat("%s");
    private final static String SINGLE_O = "%s".concat(BASIC_SEPARATOR).concat("O").concat(BASIC_SEPARATOR).concat("%s");
    private final static String SINGLE_SP = "%s".concat(BASIC_SEPARATOR).concat("SP").concat(BASIC_SEPARATOR).concat("%s");
    private final static String SINGLE_SO = "%s".concat(BASIC_SEPARATOR).concat("SO").concat(BASIC_SEPARATOR).concat("%s");
    private final static String SINGLE_PO = "%s".concat(BASIC_SEPARATOR).concat("PO").concat(BASIC_SEPARATOR).concat("%s");


    private final static String BLANK_IRI = "B".concat(IRI_SEPARATOR).concat("%s");
    private static final String BLANK_IRI_PREFIX = "B";
    private static final String SIMPLE_IRI = "S".concat(IRI_SEPARATOR).concat("%s");
    private static final String SIMPLE_IRI_PREFIX = "S";
    private static final String LITERAL_IRI = "L".concat(IRI_SEPARATOR).concat("%s").concat(IRI_SEPARATOR).concat("%s");
    private static final int IRI_PREFIX_POS = 0;
    private static final int IRI_VALUE_POS = 1;
    private static final int LITERAL_IRI_DATATYPE_POS = 2;


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
        logger.debug("#{}: Triple:{}", storeID, triple);

        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String s_iri = parseNodeIRI(subject);
            String p_iri = parseNodeIRI(predicate);
            String o_iri = parseNodeIRI(object);

            logger.debug("#{}: ({}) -> [{}] -> ({})", storeID, s_iri, p_iri, o_iri);

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

                logger.debug("#{}: Indexes=[s_{}, p_{}, o_{}]", storeID, s_idx, p_idx, o_idx);

                String sp_idx = generateComplementIndex(p_idx, s_idx);
                String so_idx = generateComplementIndex(s_idx, o_idx);
                String po_idx = generateComplementIndex(p_idx, o_idx);

                putSimpleIndex(t, ALL_S, SINGLE_S, storeID, s_idx, po_idx);
                putSimpleIndex(t, ALL_P, SINGLE_P, storeID, p_idx, so_idx);
                putSimpleIndex(t, ALL_O, SINGLE_O, storeID, o_idx, sp_idx);

                putCompoundIndex(t, SINGLE_SP, storeID, sp_idx, o_idx);
                putCompoundIndex(t, SINGLE_SO, storeID, so_idx, p_idx);
                putCompoundIndex(t, SINGLE_PO, storeID, po_idx, s_idx);

                logger.debug("#{}: Pipelined INDEX uploads.", storeID);

                t.exec();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private Response<String> getIndexFromIRI(Pipeline p, String keyFormatter, String storeID, String iri) {
        logger.info("GetIndexFromIRI: [{}, {}]", String.format(keyFormatter, storeID), iri);
        return p.hget(String.format(keyFormatter, storeID), iri);
    }

    private String getIndexFromIRI(Jedis jedis, String keyFormatter, String storeID, String iri) {
        logger.info("GetIndexFromIRI: [{}, {}]", String.format(keyFormatter, storeID), iri);
        return jedis.hget(String.format(keyFormatter, storeID), iri);
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
        logger.debug("#Split {}", Arrays.toString(split_iri));
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

    private void putCompoundIndex(Transaction transaction, String keyFormatter, String storeID, String idx, String complementIdx) {
        transaction.sadd(String.format(keyFormatter, storeID, idx), complementIdx);
    }

    private void putSimpleIndex(Transaction transaction, String allKeyFormatter, String singleKeyFormatter, String storeID, String idx, String complementIdx) {
        transaction.sadd(String.format(allKeyFormatter, storeID), idx);
        transaction.sadd(String.format(singleKeyFormatter, storeID, idx), complementIdx);
    }

    private String generateComplementIndex(String idx1, String idx2) {
        return idx1.concat(COMPOUND_INDEX_SEPARATOR).concat(idx2);
    }

    @Override
    public Iterable<Triple> getTriples(String storeID) {
        List<Triple> triples = new LinkedList<>();

        try (Jedis jedis = Redis.getCachePool().getResource()) {

            Set<String> s_idxs = jedis.smembers(String.format(ALL_S, storeID));
            Set<String> po_idxs;
            String s_iri, p_iri, o_iri;
            String p_idx, o_idx;
            String[] po_split;
            Pipeline p = jedis.pipelined();
            for (String s_idx : s_idxs) {
                po_idxs = jedis.smembers(String.format(SINGLE_S, storeID, s_idx));
                if (po_idxs != null) {
                    for (String po_idx : po_idxs) {
                        po_split = po_idx.split(COMPOUND_INDEX_SEPARATOR);
                        p_idx = po_split[0];
                        o_idx = po_split[1];
                        Response<String> resp_s_iri = p.hget(String.format(REV_S_IRIS, storeID), s_idx);
                        Response<String> resp_p_iri = p.hget(String.format(REV_P_IRIS, storeID), p_idx);
                        Response<String> resp_o_iri = p.hget(String.format(REV_O_IRIS, storeID), o_idx);
                        p.sync();

                        s_iri = resp_s_iri.get();
                        p_iri = resp_p_iri.get();
                        o_iri = resp_o_iri.get();

                        logger.debug("#{}: po->{}", storeID, po_idx);
                        logger.debug("#{}: ({}) -> [{}] -> ({})", storeID, s_iri, p_iri, o_iri);
                        logger.debug("#{}: Indexes=[s_{}, p_{}, o_{}]", storeID, s_idx, p_idx, o_idx);

                        triples.add(new Triple(
                                generateNode(s_iri),
                                generateNode(p_iri),
                                generateNode(o_iri)));
                    }
                }
            }
            return triples;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, String> getNamespaces(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            return jedis.hgetAll(String.format(NAMESPACES, storeID));
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    public Iterable<Node> findSubjects(String storeID, Node predicate, Node object) {
        return find(storeID, predicate, object, P_IRIS, O_IRIS, SINGLE_PO, REV_S_IRIS);
    }

    @Override
    public Iterable<Node> findPredicates(String storeID, Node subject, Node object) {
        return find(storeID, subject, object, S_IRIS, O_IRIS, SINGLE_SO, REV_P_IRIS);
    }

    @Override
    public Iterable<Node> findObjects(String storeID, Node subject, Node predicate) {

        return find(storeID, subject, predicate, S_IRIS, P_IRIS, SINGLE_SP, REV_O_IRIS);
    }

    private Iterable<Node> find(String storeID, Node node1, Node node2, String IRIKeyFormatter1, String IRIKeyFormatter2, String compoundIdxKeyFormatter, String reverseIRIKeyFormatter) {
        List<Node> nodes = new LinkedList<>();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<String> resp_idx_1 = getIndexFromIRI(p, IRIKeyFormatter1, storeID, parseNodeIRI(node1));
            Response<String> resp_idx_2 = getIndexFromIRI(p, IRIKeyFormatter2, storeID, parseNodeIRI(node2));
            p.sync();
            String idx_1 = resp_idx_1.get();
            String idx_2 = resp_idx_2.get();
            logger.info("{}, {}", idx_1, idx_2);
            if (idx_1 == null || idx_2 == null)
                return nodes;
            List<Response<String>> responses = new LinkedList<>();
            jedis.smembers(String.format(compoundIdxKeyFormatter, storeID, idx_1.concat(COMPOUND_INDEX_SEPARATOR)).concat(idx_2)).forEach(
                    simple_idx -> responses.add(p.hget(String.format(reverseIRIKeyFormatter, storeID), simple_idx))

            );
            p.sync();
            responses.forEach(resp -> nodes.add(generateNode(resp.get())));
            return nodes;
        } catch (Exception e) {
            e.printStackTrace();
            return nodes;
        }
    }

    @Override
    public Iterable<TypedNode> findSP(String storeID, Node object) {
        return find(storeID, object, SP);
    }

    @Override
    public Iterable<TypedNode> findSO(String storeID, Node predicate) {
        return find(storeID, predicate, SO);
    }

    @Override
    public Iterable<TypedNode> findPO(String storeID, Node subject) {
        return find(storeID, subject, PO);
    }

    private Iterable<TypedNode> find(String storeID, Node node, VariablesPattern pattern) {
        List<TypedNode> nodes = new LinkedList<>();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            String IRIKeyFormatter, reverseIRIKeyFormatter1, reverseIRIKeyFormatter2;
            VariablesPattern type1, type2;

            if (pattern.equals(SO)) {
                IRIKeyFormatter = P_IRIS;
                reverseIRIKeyFormatter1 = REV_S_IRIS;
                reverseIRIKeyFormatter2 = REV_O_IRIS;
                type1 = S;
                type2 = O;
            } else if (pattern.equals(SP)) {
                IRIKeyFormatter = O_IRIS;
                reverseIRIKeyFormatter1 = REV_S_IRIS;
                reverseIRIKeyFormatter2 = REV_P_IRIS;
                type1 = S;
                type2 = P;
            } else if (pattern.equals(PO)) {
                IRIKeyFormatter = S_IRIS;
                reverseIRIKeyFormatter1 = REV_P_IRIS;
                reverseIRIKeyFormatter2 = REV_O_IRIS;
                type1 = P;
                type2 = O;
            } else {
                return null;
            }
            String idx = getIndexFromIRI(jedis, IRIKeyFormatter, storeID, parseNodeIRI(node));
            if (idx == null)
                return nodes;
            List<Response<String>> responses1 = new LinkedList<>();
            List<Response<String>> responses2 = new LinkedList<>();
            jedis.smembers(idx).forEach(
                    compound_idx -> {
                        logger.info("{}", compound_idx);
                        String[] simple_idxs = compound_idx.split(COMPOUND_INDEX_SEPARATOR);
                        responses1.add(p.hget(String.format(reverseIRIKeyFormatter1, storeID), simple_idxs[0]));
                        responses2.add(p.hget(String.format(reverseIRIKeyFormatter2, storeID), simple_idxs[1]));
                    }
            );
            p.sync();
            responses1.forEach(resp -> nodes.add(new TypedNode(type1, generateNode(resp.get()))));
            responses2.forEach(resp -> nodes.add(new TypedNode(type2, generateNode(resp.get()))));
            nodes.forEach(System.out::println);
            return nodes;
        } catch (Exception e) {
            e.printStackTrace();
            return nodes;
        }
    }
}
