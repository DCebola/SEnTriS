package pt.fct.nova.id.srv.application.storage.redis;

import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.application.storage.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.storage.StorageEngine;
import pt.fct.nova.id.srv.application.storage.tables.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;

import static pt.fct.nova.id.srv.application.Utils.generateID;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class RedisDefaultStorageEngine implements StorageEngine {

    private static final String BASIC_SEPARATOR = System.getenv("BASIC_SEPARATOR");
    private static final String NODE_SEPARATOR = System.getenv("NODE_SEPARATOR");
    private static final String COMPOUND_NODE_SEPARATOR = System.getenv("COMPOUND_NODE_SEPARATOR");
    private static final String BLANK_PREFIX = "B";
    private static final String SIMPLE_PREFIX = "S";
    private static final String LITERAL_PREFIX = "L";
    private static final String SIMPLE_NODE = SIMPLE_PREFIX.concat(NODE_SEPARATOR).concat("%s");
    private static final String LITERAL_NODE = LITERAL_PREFIX.concat(NODE_SEPARATOR).concat("%s").concat(NODE_SEPARATOR).concat("%s");
    private final static String BLANK_NODE = BLANK_PREFIX.concat(NODE_SEPARATOR).concat("%s");
    private static final int PREFIX_POS = 0;
    private static final int VALUE_POS = 1;
    private static final int LITERAL_DATATYPE_POS = 2;
    private static final String TRIPLESTORE_DATA_PATTERN = "%s".concat(BASIC_SEPARATOR).concat("*");
    private static final String BLANK = "BLANK";
    private static final String SCHEMA_KEYWORD_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("SCHEMA");

    private static final String TRIPLE = "%s".concat(COMPOUND_NODE_SEPARATOR).concat("%s").concat(COMPOUND_NODE_SEPARATOR).concat("%s");
    private static final String COMPOUND_KEYWORD = "%s".concat(BASIC_SEPARATOR).concat("%s");
    private static final String COMPOUND_NODE = "%s".concat(COMPOUND_NODE_SEPARATOR).concat("%s");
    private static final String KEYWORD_FORMAT = "%s".concat(BASIC_SEPARATOR).concat("%s").concat(BASIC_SEPARATOR).concat("%s");

    @Override
    public void delete(String triplestoreID, boolean schema) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            if (schema)
                t.del(String.format(SCHEMA_KEYWORD_FORMAT, triplestoreID));
            else
                Redis.scan(jedis, TRIPLESTORE_DATA_PATTERN).forEach(t::del);
            t.exec();
        }
    }

    @Override
    public void delete(String triplestoreID, Set<Triple> triples) throws InvalidNodeException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String s, p, o, s_keyword, p_keyword, o_keyword;
            for (Triple triple : triples) {
                s = parseNode(triple.getSubject());
                p = parseNode(triple.getPredicate());
                o = parseNode(triple.getObject());
                s_keyword = parseKeyword(s);
                p_keyword = parseKeyword(p);
                o_keyword = parseKeyword(o);
                delete(t, triplestoreID, String.format(COMPOUND_NODE, p, o), PO, s_keyword);
                delete(t, triplestoreID, String.format(COMPOUND_NODE, s, o), SO, p_keyword);
                delete(t, triplestoreID, String.format(COMPOUND_NODE, s, p), SP, o_keyword);
                delete(t, triplestoreID, s, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
                delete(t, triplestoreID, p, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
                delete(t, triplestoreID, o, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
            }
            t.exec();
        }
    }

    private void delete(Transaction t, String triplestoreID, String node, VariablesPattern pattern, String keyword) {
        t.srem(String.format(KEYWORD_FORMAT, triplestoreID, pattern, keyword), node);
    }

    @Override
    public void save(String triplestoreID, Set<Triple> triples) throws InvalidNodeException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String s, p, o, s_keyword, p_keyword, o_keyword;
            for (Triple triple : triples) {
                s = parseNode(triple.getSubject());
                p = parseNode(triple.getPredicate());
                o = parseNode(triple.getObject());
                s_keyword = parseKeyword(s);
                p_keyword = parseKeyword(p);
                o_keyword = parseKeyword(o);
                save(t, triplestoreID, String.format(COMPOUND_NODE, p, o), PO, s_keyword);
                save(t, triplestoreID, String.format(COMPOUND_NODE, s, o), SO, p_keyword);
                save(t, triplestoreID, String.format(COMPOUND_NODE, s, p), SP, o_keyword);
                save(t, triplestoreID, s, S, String.format(COMPOUND_KEYWORD, p_keyword, o_keyword));
                save(t, triplestoreID, p, P, String.format(COMPOUND_KEYWORD, s_keyword, o_keyword));
                save(t, triplestoreID, o, O, String.format(COMPOUND_KEYWORD, s_keyword, p_keyword));
            }
            t.exec();
        }
    }

    @Override
    public void saveSchema(String triplestoreID, Set<Triple> triples) throws InvalidNodeException {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Transaction t = jedis.multi();
            String s, p, o;
            for (Triple triple : triples) {
                s = parseNode(triple.getSubject());
                p = parseNode(triple.getPredicate());
                o = parseNode(triple.getObject());
                t.sadd(String.format(SCHEMA_KEYWORD_FORMAT, triplestoreID), String.format(TRIPLE, s, p, o));
            }
            t.exec();
        }
    }

    @Override
    public Set<Triple> findSchema(String triplestoreID) {
        Set<Triple> schema = new HashSet<>();
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            Set<String> serialized = jedis.smembers(String.format(SCHEMA_KEYWORD_FORMAT, triplestoreID));
            String[] nodes;
            for (String t : serialized) {
                nodes = t.split(COMPOUND_NODE_SEPARATOR);
                schema.add(new Triple(
                        generateNode(nodes[0]),
                        generateNode(nodes[1]),
                        generateNode(nodes[2])
                ));
            }
        }
        return schema;
    }

    private void save(Transaction t, String triplestoreID, String node, VariablesPattern pattern, String keyword) {
        t.sadd(String.format(KEYWORD_FORMAT, triplestoreID, pattern, keyword), node);
    }

    private String parseKeyword(String node) {
        if (node.split(NODE_SEPARATOR)[PREFIX_POS].equals(BLANK_PREFIX))
            return BLANK;
        return node;
    }

    private String parseKeyword(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_NODE, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_NODE, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return BLANK;
    }

    @Override
    public Node generateNode(String node) {
        String[] split = node.split(NODE_SEPARATOR);
        if (split[PREFIX_POS].equals(BLANK_PREFIX))
            return NodeFactory.createBlankNode(split[VALUE_POS]);
        else if (split[PREFIX_POS].equals(SIMPLE_PREFIX))
            return NodeFactory.createURI(split[VALUE_POS]);
        else
            return NodeFactory.createLiteral(
                    split[VALUE_POS],
                    TypeMapper.getInstance().getSafeTypeByName(split[LITERAL_DATATYPE_POS]));
    }

    @Override
    public String parseNode(Node node) throws InvalidNodeException {
        if (!node.isConcrete())
            throw new InvalidNodeException();
        if (node.isURI())
            return String.format(SIMPLE_NODE, node.getURI());
        else if (node.isLiteral())
            return String.format(LITERAL_NODE, node.getLiteralLexicalForm(), node.getLiteralDatatypeURI());
        else
            return String.format(BLANK_NODE, node.getBlankNodeId());
    }

    @Override
    public BindingsTable findS(String triplestoreID, Node predicate, Node object, Var subject) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(predicate), parseKeyword(object));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, S, keyword), subject);
    }

    @Override
    public BindingsTable findP(String triplestoreID, Node subject, Node object, Var predicate) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(subject), parseKeyword(object));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, P, keyword), predicate);
    }

    @Override
    public BindingsTable findO(String triplestoreID, Node subject, Node predicate, Var object) throws InvalidNodeException {
        String keyword = String.format(COMPOUND_KEYWORD, parseKeyword(subject), parseKeyword(predicate));
        return find(String.format(KEYWORD_FORMAT, triplestoreID, O, keyword), object);
    }

    private BindingsTable find(String keyword, Var var) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTable res = new MemBindingsTable(var);
            Set<String> nodes = jedis.smembers(keyword);
            nodes.forEach(node -> res.add(generateID(), var, node));
            System.out.println(keyword + " | " + var.getVarName() + " | " + res.getPatterns().size());
            return res;
        }
    }

    @Override
    public BindingsTable findSP(String triplestoreID, Node object, Var subject, Var predicate) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, SP, parseKeyword(object)), subject, predicate);
    }

    @Override
    public BindingsTable findSO(String triplestoreID, Node predicate, Var subject, Var object) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, SO, parseKeyword(predicate)), subject, object);
    }

    @Override
    public BindingsTable findPO(String triplestoreID, Node subject, Var predicate, Var object) throws InvalidNodeException {
        return find(String.format(KEYWORD_FORMAT, triplestoreID, PO, parseKeyword(subject)), predicate, object);
    }

    private BindingsTable find(String keyword, Var var1, Var var2) {
        try (Jedis jedis = Redis.getCachePool().getResource()) {
            BindingsTable res = new MemBindingsTable(var1, var2);
            Set<String> compound_nodes = jedis.smembers(keyword);
            compound_nodes.forEach(
                    node -> {
                        String[] nodes = node.split(COMPOUND_NODE_SEPARATOR);
                        String p_idx = generateID();
                        res.add(p_idx, var1, nodes[0]);
                        res.add(p_idx, var2, nodes[1]);
                    }
            );
            System.out.println(keyword + " | " + var1.getVarName() + " | " + var2.getVarName() + " | " + res.getPatterns().size());
            return res;
        }
    }
}
