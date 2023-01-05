package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.iri_tables.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class RedisDefaultStorageEngine implements StorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private static final String IRI_SEPARATOR = System.getenv("IRI_SEPARATOR");
    private static final String COMPOUND_IRI_SEPARATOR = System.getenv("COMPOUND_IRI_SEPARATOR");
    private static final String BLANK_IRI_PREFIX = "B";
    private static final String SIMPLE_IRI_PREFIX = "S";
    private static final String LITERAL_IRI_PREFIX = "L";
    private static final String SIMPLE_IRI = SIMPLE_IRI_PREFIX.concat(IRI_SEPARATOR).concat("%s");
    private static final String LITERAL_IRI = LITERAL_IRI_PREFIX.concat(IRI_SEPARATOR).concat("%s").concat(IRI_SEPARATOR).concat("%s");
    private final static String BLANK_IRI = BLANK_IRI_PREFIX.concat(IRI_SEPARATOR).concat("%s");
    private static final int IRI_PREFIX_POS = 0;
    private static final int IRI_VALUE_POS = 1;
    private static final int LITERAL_IRI_DATATYPE_POS = 2;
    private static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");
    private static final String BLANK = "BLANK";
    private static final String COMPOUND_KEYWORD = "%s".concat(BASIC_SEPARATOR).concat("%s");
    private static final String COMPOUND_IRI = "%s".concat(COMPOUND_IRI_SEPARATOR).concat("%s");
    private static final String KEYWORD_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("%s");


    @Override
    public void delete(String triplestoreID) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            Redis.scan(jedis, TRIPLESTORE_DATA_PATTERN).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void delete(String triplestoreID, List<String[]> triples) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (String[] triple : triples) {
                System.out.println("Deleting" + Arrays.toString(triple));
                String s = triple[0];
                String p = triple[1];
                String o = triple[2];
                String s_keyword, p_keyword, o_keyword;
                s_keyword = parseKeyword(s);
                p_keyword = parseKeyword(p);
                o_keyword = parseKeyword(o);
                delete(t, triplestoreID, String.format(COMPOUND_IRI, p, o), PO, s_keyword);
                delete(t, triplestoreID, String.format(COMPOUND_IRI, s, o), SO, p_keyword);
                delete(t, triplestoreID, String.format(COMPOUND_IRI, s, p), SP, o_keyword);
                delete(t, triplestoreID, s, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
                delete(t, triplestoreID, p, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
                delete(t, triplestoreID, o, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
            }
            t.exec();
        }
    }

    private void delete(Transaction t, String triplestoreID, String iri, VariablesPattern pattern, String keyword) {
        t.srem(String.format(KEYWORD_FORMAT, triplestoreID, pattern, keyword), iri);
    }

    @Override
    public void save(String triplestoreID, List<String[]> triples) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            for (String[] triple : triples) {
                String s = triple[0];
                String p = triple[1];
                String o = triple[2];
                String s_keyword, p_keyword, o_keyword;
                s_keyword = parseKeyword(s);
                p_keyword = parseKeyword(p);
                o_keyword = parseKeyword(o);
                save(t, triplestoreID, String.format(COMPOUND_IRI, p, o), PO, s_keyword);
                save(t, triplestoreID, String.format(COMPOUND_IRI, s, o), SO, p_keyword);
                save(t, triplestoreID, String.format(COMPOUND_IRI, s, p), SP, o_keyword);
                save(t, triplestoreID, s, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
                save(t, triplestoreID, p, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
                save(t, triplestoreID, o, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
            }
            t.exec();
        }
    }

    private void save(Transaction t, String triplestoreID, String iri, VariablesPattern pattern, String keyword) {
        t.sadd(String.format(KEYWORD_FORMAT, triplestoreID, pattern, keyword), iri);
    }

    private String parseKeyword(String iri) {
        if (iri.split(IRI_SEPARATOR)[IRI_PREFIX_POS].equals(BLANK_IRI_PREFIX))
            return BLANK;
        return iri;
    }

    private String parseKeyword(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_IRI, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_IRI, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return BLANK;
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
    public IRITable findS(String triplestoreID, Node predicate, Node object, Var subject) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(predicate), parseKeyword(object));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, S, keyword), subject);
    }

    @Override
    public IRITable findP(String triplestoreID, Node subject, Node object, Var predicate) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(subject), parseKeyword(object));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, P, keyword), predicate);
    }

    @Override
    public IRITable findO(String triplestoreID, Node subject, Node predicate, Var object) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(subject), parseKeyword(predicate));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, O, keyword), object);
    }

    private IRITable find(String keyword, Var var) {
        IRITable res = new MemIRITable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> iris = jedis.smembers(keyword);
            iris.forEach(iri -> res.add(generateID(), var, iri));
            System.out.println(keyword + " | " + var.getVarName() + " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            return res;
        }
    }

    @Override
    public IRITable findSP(String triplestoreID, Node object, Var subject, Var predicate) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, SP, parseKeyword(object)), subject, predicate);
    }

    @Override
    public IRITable findSO(String triplestoreID, Node predicate, Var subject, Var object) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, SO, parseKeyword(predicate)), subject, object);
    }

    @Override
    public IRITable findPO(String triplestoreID, Node subject, Var predicate, Var object) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, PO, parseKeyword(subject)), predicate, object);
    }

    private IRITable find(String keyword, Var var1, Var var2) {
        IRITable res = new MemIRITable();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> compound_iris = jedis.smembers(keyword);
            compound_iris.forEach(
                    compound_iri -> {
                        String[] iris = compound_iri.split(COMPOUND_IRI_SEPARATOR);
                        System.out.println(compound_iri);
                        String p_idx = generateID();
                        res.add(p_idx, var1, iris[0]);
                        res.add(p_idx, var2, iris[1]);
                    }
            );
            System.out.println(keyword + " | " + var1.getVarName() + " | " + var2.getVarName()+ " | " + res.getPatterns().size());
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }
    }
}
