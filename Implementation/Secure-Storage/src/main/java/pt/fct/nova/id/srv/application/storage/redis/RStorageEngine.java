package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.storage.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.idx_data_structs.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;

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

    private final static String REV_IRIS = "%s".concat(BASIC_SEPARATOR).concat("REV_IRIS");

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
                    putIRI(t, S_IRIS, storeID, s_iri, s_idx);
                }
                if (p_idx == null) {
                    p_idx = generateID();
                    putIRI(t, P_IRIS, storeID, p_iri, p_idx);
                }
                if (o_idx == null) {
                    o_idx = generateID();
                    putIRI(t, O_IRIS, storeID, o_iri, o_idx);
                }

                logger.debug("#{}: Indexes=[s_{}, p_{}, o_{}]", storeID, s_idx, p_idx, o_idx);

                String sp_idx = generateComplementIndex(s_idx, p_idx);
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
        logger.debug("GetIndexFromIRI: [{}, {}]", String.format(keyFormatter, storeID), iri);
        return p.hget(String.format(keyFormatter, storeID), iri);
    }

    private String getIndexFromIRI(Jedis jedis, String keyFormatter, String storeID, String iri) {
        logger.debug("GetIndexFromIRI: [{}, {}]", String.format(keyFormatter, storeID), iri);
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
        if (split_iri[IRI_PREFIX_POS].equals(BLANK_IRI_PREFIX))
            return NodeFactory.createBlankNode(split_iri[IRI_VALUE_POS]);
        else if (split_iri[IRI_PREFIX_POS].equals(SIMPLE_IRI_PREFIX))
            return NodeFactory.createURI(split_iri[IRI_VALUE_POS]);
        else
            return NodeFactory.createLiteral(
                    split_iri[IRI_VALUE_POS],
                    TypeMapper.getInstance().getSafeTypeByName(split_iri[LITERAL_IRI_DATATYPE_POS]));
    }

    private void putIRI(Transaction transaction, String keyFormatter, String storeID, String nodeIRI, String idx) {
        transaction.hset(String.format(keyFormatter, storeID), nodeIRI, idx);
        transaction.hset(String.format(REV_IRIS, storeID), idx, nodeIRI);
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
    public List<Triple> getTriples(String storeID) {
        List<Triple> triples = new LinkedList<>();
        try (Jedis jedis = Redis.getCachePool().getResource()) {

            Set<String> s_idxs = jedis.smembers(String.format(ALL_S, storeID));
            List<Response<Set<String>>> responses = new ArrayList<>(s_idxs.size());

            Pipeline p = jedis.pipelined();

            for (String s_idx : s_idxs)
                responses.add(p.smembers(String.format(SINGLE_S, storeID, s_idx)));

            p.sync();
            int i = 0;
            for (String s_idx : s_idxs) {
                responses.get(i).get().forEach(
                        po_idx -> {
                            String[] po_split = po_idx.split(COMPOUND_INDEX_SEPARATOR);
                            String p_idx = po_split[0];
                            String o_idx = po_split[1];
                            Response<String> resp_s_iri = p.hget(String.format(REV_IRIS, storeID), s_idx);
                            Response<String> resp_p_iri = p.hget(String.format(REV_IRIS, storeID), p_idx);
                            Response<String> resp_o_iri = p.hget(String.format(REV_IRIS, storeID), o_idx);
                            p.sync();

                            String s_iri = resp_s_iri.get();
                            String p_iri = resp_p_iri.get();
                            String o_iri = resp_o_iri.get();

                            triples.add(new Triple(
                                    generateNode(s_iri),
                                    generateNode(p_iri),
                                    generateNode(o_iri)));
                        }
                );
                i++;
            }

            return triples;
        } catch (Exception e) {
            return triples;
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
    public IdxTable findSubjects(String storeID, Node predicate, Node object, Var var) {
        return find(storeID, predicate, object, P_IRIS, O_IRIS, SINGLE_PO, var);
    }

    @Override
    public IdxTable findPredicates(String storeID, Node subject, Node object, Var var) {
        return find(storeID, subject, object, S_IRIS, O_IRIS, SINGLE_SO, var);
    }

    @Override
    public IdxTable findObjects(String storeID, Node subject, Node predicate, Var var) {
        return find(storeID, subject, predicate, S_IRIS, P_IRIS, SINGLE_SP, var);
    }

    private IdxTable find(String storeID, Node node1, Node node2, String IRIKeyFormatter1, String IRIKeyFormatter2, String compoundIdxKeyFormatter, Var var) {
        IdxTable res = new MemIdxTable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<String> resp_idx_1 = getIndexFromIRI(p, IRIKeyFormatter1, storeID, parseNodeIRI(node1));
            Response<String> resp_idx_2 = getIndexFromIRI(p, IRIKeyFormatter2, storeID, parseNodeIRI(node2));
            p.sync();
            String idx_1 = resp_idx_1.get();
            String idx_2 = resp_idx_2.get();
            logger.info("{}, {}", idx_1, idx_2);
            if (idx_1 == null || idx_2 == null)
                return res;
            jedis.smembers(String.format(compoundIdxKeyFormatter, storeID, idx_1.concat(COMPOUND_INDEX_SEPARATOR)).concat(idx_2)).forEach(
                    idx -> res.addIdx(generateID(), var, idx)
            );
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }
    }

    @Override
    public IdxTable findAll(String storeID, Var var1, Var var2, Var var3) {
        IdxTable res = new MemIdxTable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {

            Set<String> s_idxs = jedis.smembers(String.format(ALL_S, storeID));
            int total_triples = s_idxs.size();

            List<Response<Set<String>>> responses = new ArrayList<>(total_triples);

            Pipeline p = jedis.pipelined();

            for (String s_idx : s_idxs)
                responses.add(p.smembers(String.format(SINGLE_S, storeID, s_idx)));

            p.sync();
            int i = 0;
            for (String s_idx : s_idxs) {
                responses.get(i).get().forEach(
                        po_idx -> {
                            String[] po_split = po_idx.split(COMPOUND_INDEX_SEPARATOR);
                            String t_idx = generateID();
                            res.addIdx(t_idx, var1, s_idx);
                            res.addIdx(t_idx, var2, po_split[0]);
                            res.addIdx(t_idx, var3, po_split[1]);
                        }
                );
                i++;
            }
            return res;
        } catch (Exception e) {
            return res;
        }
    }

    @Override
    public List<Binding> fetchNodes(String storeID, IdxTable idxTable) {
        List<Binding> res = new LinkedList<>();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();

            Set<List<String>> patterns = idxTable.getPatterns();
            int num_patterns = patterns.size();
            List<List<Response<String>>> responses = new ArrayList<>(num_patterns);

            List<Response<String>> l;
            for (List<String> pattern : patterns) {
                l = new LinkedList<>();
                for (String idx : pattern)
                    l.add(p.hget(String.format(REV_IRIS, storeID), idx));
                responses.add(l);
            }

            p.sync();

            int j = 0;
            List<Var> vars = new ArrayList<>(idxTable.getVars());
            BindingBuilder builder = Binding.builder();
            for (int i = 0; i < num_patterns; i++) {
                for (Response<String> r : responses.get(i)) {
                    builder.add(vars.get(j), generateNode(r.get()));
                    j++;
                }
                res.add(builder.build());
                j = 0;
                builder.reset();
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }
    }

    @Override
    public IdxTable findSP(String storeID, Node object, Var var1, Var var2) {
        return find(storeID, object, O_IRIS, SINGLE_O, var1, var2);
    }

    @Override
    public IdxTable findSO(String storeID, Node predicate, Var var1, Var var2) {
        return find(storeID, predicate, P_IRIS, SINGLE_P, var1, var2);
    }

    @Override
    public IdxTable findPO(String storeID, Node subject, Var var1, Var var2) {
        return find(storeID, subject, S_IRIS, SINGLE_S, var1, var2);
    }

    private IdxTable find(String storeID, Node node, String IRIKeyFormatter, String idxKeyFormatter, Var var1, Var var2) {
        IdxTable res = new MemIdxTable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String idx = getIndexFromIRI(jedis, IRIKeyFormatter, storeID, parseNodeIRI(node));
            if (idx == null)
                return res;
            jedis.smembers(String.format(idxKeyFormatter, storeID, idx)).forEach(
                    compound_idx -> {
                        String[] simple_idxs = compound_idx.split(COMPOUND_INDEX_SEPARATOR);
                        String t_idx = generateID();
                        res.addIdx(t_idx, var1, simple_idxs[0]);
                        res.addIdx(t_idx, var2, simple_idxs[1]);
                    }
            );
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }
    }
}
