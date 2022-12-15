package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.iri_tables.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;

public class RedisDefaultStorageEngine implements StorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private static final String COMPOUND_INDEX_SEPARATOR = System.getenv("COMPOUND_INDEX_SEPARATOR");
    private static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");

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
    private static final String STORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");

    @Override
    public void deleteStore(String storeID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
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
            collector.forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void saveTriple(String storeID, String[] triple) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String s_iri = triple[0];
            String p_iri = triple[1];
            String o_iri = triple[2];

            Pipeline p = jedis.pipelined();
            Response<String> resp_s = getIndexFromIRI(p, S_IRIS, storeID, s_iri);
            Response<String> resp_p = getIndexFromIRI(p, P_IRIS, storeID, p_iri);
            Response<String> resp_o = getIndexFromIRI(p, O_IRIS, storeID, o_iri);
            p.sync();

            String s_idx = resp_s.get();
            String p_idx = resp_p.get();
            String o_idx = resp_o.get();

            Transaction t = jedis.multi();

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

            String sp_idx = generateComplementIndex(s_idx, p_idx);
            String so_idx = generateComplementIndex(s_idx, o_idx);
            String po_idx = generateComplementIndex(p_idx, o_idx);

            putSimpleIndex(t, ALL_S, SINGLE_S, storeID, s_idx, po_idx);
            putSimpleIndex(t, ALL_P, SINGLE_P, storeID, p_idx, so_idx);
            putSimpleIndex(t, ALL_O, SINGLE_O, storeID, o_idx, sp_idx);

            putCompoundIndex(t, SINGLE_SP, storeID, sp_idx, o_idx);
            putCompoundIndex(t, SINGLE_SO, storeID, so_idx, p_idx);
            putCompoundIndex(t, SINGLE_PO, storeID, po_idx, s_idx);

            t.exec();
        }
    }


    private Response<String> getIndexFromIRI(Pipeline p, String keyFormatter, String storeID, String iri) {
        return p.hget(String.format(keyFormatter, storeID), iri);
    }

    private String getIndexFromIRI(Jedis jedis, String keyFormatter, String storeID, String iri) {
        return jedis.hget(String.format(keyFormatter, storeID), iri);
    }


    @Override
    public String parseNodeIRI(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_IRI, node.getBlankNodeId());
    }

    @Override
    public Node generateNode(String iri) {
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
    public IRITable findSubjects(String storeID, Node predicate, Node object, Var var) {
        return find(storeID, predicate, object, P_IRIS, O_IRIS, SINGLE_PO, var);
    }

    @Override
    public IRITable findPredicates(String storeID, Node subject, Node object, Var var) {
        return find(storeID, subject, object, S_IRIS, O_IRIS, SINGLE_SO, var);
    }

    @Override
    public IRITable findObjects(String storeID, Node subject, Node predicate, Var var) {
        return find(storeID, subject, predicate, S_IRIS, P_IRIS, SINGLE_SP, var);
    }

    private IRITable find(String storeID, Node node1, Node node2, String IRIKeyFormatter1, String IRIKeyFormatter2, String compoundIdxKeyFormatter, Var var) {
        IRITable res = new MemIRITable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Pipeline p = jedis.pipelined();
            Response<String> resp_idx_1 = getIndexFromIRI(p, IRIKeyFormatter1, storeID, parseNodeIRI(node1));
            Response<String> resp_idx_2 = getIndexFromIRI(p, IRIKeyFormatter2, storeID, parseNodeIRI(node2));
            p.sync();
            String idx_1 = resp_idx_1.get();
            String idx_2 = resp_idx_2.get();
            if (idx_1 == null || idx_2 == null)
                return res;
            System.out.printf(compoundIdxKeyFormatter, storeID, idx_1.concat(COMPOUND_INDEX_SEPARATOR).concat(idx_2));
            Set<String> idxs = jedis.smembers(String.format(compoundIdxKeyFormatter, storeID, idx_1.concat(COMPOUND_INDEX_SEPARATOR).concat(idx_2)));
            List<Response<String>> responses = new ArrayList<>(idxs.size());

            int i = 0;
            for (String idx : idxs) {
                responses.add(i, p.hget(String.format(REV_IRIS, storeID), idx));
                i++;
            }
            p.sync();
            responses.forEach(r -> res.add(generateID(), var, r.get()));

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }
    }

    @Override
    public IRITable findAll(String storeID, Var var1, Var var2, Var var3) {
        IRITable res = new MemIRITable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {

            Set<String> s_idxs = jedis.smembers(String.format(ALL_S, storeID));
            int total_triples = s_idxs.size();

            List<Response<Set<String>>> s_idxs_responses = new ArrayList<>(total_triples);

            Pipeline p = jedis.pipelined();

            for (String s_idx : s_idxs)
                s_idxs_responses.add(p.smembers(String.format(SINGLE_S, storeID, s_idx)));

            p.sync();

            List<List<Response<String>>> iri_responses = new LinkedList<>();

            int i = 0;
            for (String s_idx : s_idxs) {
                s_idxs_responses.get(i).get().forEach(
                        po_idx -> {
                            String[] po_split = po_idx.split(COMPOUND_INDEX_SEPARATOR);
                            List<Response<String>> iris = new ArrayList<>(3);
                            iris.add(p.hget(String.format(REV_IRIS, storeID), s_idx));
                            iris.add(p.hget(String.format(REV_IRIS, storeID), po_split[0]));
                            iris.add(p.hget(String.format(REV_IRIS, storeID), po_split[1]));
                            iri_responses.add(iris);
                        }
                );
                i++;
            }

            p.sync();

            iri_responses.forEach(
                    resp_list -> {
                        String p_idx = generateID();
                        res.add(p_idx, var1, resp_list.get(0).get());
                        res.add(p_idx, var2, resp_list.get(1).get());
                        res.add(p_idx, var3, resp_list.get(2).get());
                    }
            );
            return res;
        }
    }

    @Override
    public IRITable findSP(String storeID, Node object, Var var1, Var var2) {
        return find(storeID, object, O_IRIS, SINGLE_O, var1, var2);
    }

    @Override
    public IRITable findSO(String storeID, Node predicate, Var var1, Var var2) {
        return find(storeID, predicate, P_IRIS, SINGLE_P, var1, var2);
    }

    @Override
    public IRITable findPO(String storeID, Node subject, Var var1, Var var2) {
        return find(storeID, subject, S_IRIS, SINGLE_S, var1, var2);
    }

    private IRITable find(String storeID, Node node, String IRIKeyFormatter, String idxKeyFormatter, Var var1, Var var2) {
        IRITable res = new MemIRITable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            String idx = getIndexFromIRI(jedis, IRIKeyFormatter, storeID, parseNodeIRI(node));
            if (idx == null)
                return res;

            Pipeline p = jedis.pipelined();

            List<List<Response<String>>> iri_responses = new LinkedList<>();

            jedis.smembers(String.format(idxKeyFormatter, storeID, idx)).forEach(
                    compound_idx -> {
                        String[] simple_idxs = compound_idx.split(COMPOUND_INDEX_SEPARATOR);
                        List<Response<String>> iris = new ArrayList<>(2);
                        iris.add(p.hget(String.format(REV_IRIS, storeID), simple_idxs[0]));
                        iris.add(p.hget(String.format(REV_IRIS, storeID), simple_idxs[1]));
                        iri_responses.add(iris);
                    }
            );

            p.sync();

            iri_responses.forEach(
                    resp_list -> {
                        String p_idx = generateID();
                        res.add(p_idx, var1, resp_list.get(0).get());
                        res.add(p_idx, var2, resp_list.get(1).get());
                    }
            );
            return res;
        } catch (InvalidNodeException e) {
            return res;
        }
    }
}
