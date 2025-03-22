package pt.fct.nova.id.srv.application.query;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.graph.GraphFactory;
import pt.fct.nova.id.srv.application.query.jobs.SearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.nio.ByteBuffer;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafe;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class QueryUtils {
    public static VariablesPattern extractVariablesPattern(Node subject, Node predicate, Node object) {
        if (subject.isVariable() && !predicate.isVariable() && !object.isVariable())
            return S;
        else if (!subject.isVariable() && predicate.isVariable() && !object.isVariable())
            return P;
        else if (!subject.isVariable() && !predicate.isVariable() && object.isVariable())
            return O;
        else if (subject.isVariable() && predicate.isVariable() && !object.isVariable())
            return SP;
        else if (subject.isVariable() && !predicate.isVariable() && object.isVariable())
            return SO;
        else if (!subject.isVariable() && predicate.isVariable() && object.isVariable())
            return PO;
        else
            return SPO;
    }

    public static Set<Var> extractVars(SearchJob job) {
        Node s = job.getSubject();
        Node p = job.getPredicate();
        Node o = job.getObject();
        Set<Var> res = new HashSet<>();
        switch (job.getVariablesPattern()) {
            case S -> res.add(Var.alloc(s));
            case P -> res.add(Var.alloc(p));
            case O -> res.add(Var.alloc(o));
            case SP -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(p));
            }
            case SO -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(o));
            }
            case PO -> {
                res.add(Var.alloc(p));
                res.add(Var.alloc(o));
            }
            case SPO -> {
                res.add(Var.alloc(s));
                res.add(Var.alloc(p));
                res.add(Var.alloc(o));
            }
        }
        return res;
    }

    public static String generateID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return encodeBase64URLSafeString(bb.array());
    }

    public static QueryType convertQueryType(org.apache.jena.query.QueryType queryType) {
        if (queryType == org.apache.jena.query.QueryType.SELECT)
            return QueryType.SELECT;
        else if (queryType == org.apache.jena.query.QueryType.CONSTRUCT)
            return QueryType.CONSTRUCT;
        else if (queryType == org.apache.jena.query.QueryType.ASK)
            return QueryType.ASK;
        else if (queryType == org.apache.jena.query.QueryType.DESCRIBE)
            return QueryType.DESCRIBE;
        else throw new QueryParseException("Unknown type: ".concat(queryType.name()), 0, 0);
    }

    public static Graph generateGraphFromBindings(List<Triple> template, Collection<Binding> bindings) {
        Graph g = GraphFactory.createDefaultGraph();
        Node s, p, o;
        Node n1, n2;
        for (Binding binding : bindings) {
            for (Triple t : template) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (QueryUtils.extractVariablesPattern(s, p, o)) {
                    case S -> {
                        n1 = binding.get(Var.alloc(s));
                        if (n1 != null)
                            g.add(Triple.create(n1, p, o));
                    }
                    case P -> {
                        n1 = binding.get(Var.alloc(p));
                        if (n1 != null)
                            g.add(Triple.create(s, n1, o));
                    }
                    case O -> {
                        n1 = binding.get(Var.alloc(o));
                        if (n1 != null)
                            g.add(Triple.create(s, p, n1));
                    }
                    case SP -> {
                        n1 = binding.get(Var.alloc(s));
                        n2 = binding.get(Var.alloc(p));
                        if (n1 != null && n2 != null)
                            g.add(Triple.create(n1, n2, o));
                    }
                    case SO -> {
                        n1 = binding.get(Var.alloc(s));
                        n2 = binding.get(Var.alloc(o));
                        if (n1 != null && n2 != null)
                            g.add(Triple.create(n1, p, n2));
                    }
                    case PO -> {
                        n1 = binding.get(Var.alloc(p));
                        n2 = binding.get(Var.alloc(o));
                        if (n1 != null && n2 != null)
                            g.add(Triple.create(s, n1, n2));
                    }
                    case SPO -> g.add(t);
                }
            }
        }
        return g;
    }

    public static Graph generateGraphFromSerializableBindings(List<Triple> template, Collection<SerializableBinding<String>> bindings) {
        Graph g = GraphFactory.createDefaultGraph();
        Node s, p, o;
        String val1, val2;
        for (SerializableBinding<String> binding : bindings) {
            for (Triple t : template) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (QueryUtils.extractVariablesPattern(s, p, o)) {
                    case S -> {
                        val1 = binding.get(Var.alloc(s));
                        if (val1 != null)
                            g.add(Triple.create(ParsingUtils.generateNode(val1), p, o));
                    }
                    case P -> {
                        val1 = binding.get(Var.alloc(p));
                        if (val1 != null)
                            g.add(Triple.create(s, ParsingUtils.generateNode(val1), o));
                    }
                    case O -> {
                        val1 = binding.get(Var.alloc(o));
                        if (val1 != null)
                            g.add(Triple.create(s, p, ParsingUtils.generateNode(val1)));
                    }
                    case SP -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(p));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(ParsingUtils.generateNode(val1), ParsingUtils.generateNode(val2), o));
                    }
                    case SO -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(ParsingUtils.generateNode(val1), p, ParsingUtils.generateNode(val2)));
                    }
                    case PO -> {
                        val1 = binding.get(Var.alloc(p));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(s, ParsingUtils.generateNode(val1), ParsingUtils.generateNode(val2)));
                    }
                    case SPO -> g.add(t);
                }
            }
        }
        return g;
    }

    public static Set<Triple> generateTriplesFromSerializableBindings(Set<Triple> template, Collection<SerializableBinding<String>> bindings) {
        Set<Triple> triples = new HashSet<>();
        Node s, p, o;
        String val1, val2;
        for (SerializableBinding<String> binding : bindings) {
            for (Triple t : template) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (QueryUtils.extractVariablesPattern(s, p, o)) {
                    case S -> {
                        val1 = binding.get(Var.alloc(s));
                        if (val1 != null)
                            triples.add(Triple.create(ParsingUtils.generateNode(val1), p, o));
                    }
                    case P -> {
                        val1 = binding.get(Var.alloc(p));
                        if (val1 != null)
                            triples.add(Triple.create(s, ParsingUtils.generateNode(val1), o));
                    }
                    case O -> {
                        val1 = binding.get(Var.alloc(o));
                        if (val1 != null)
                            triples.add(Triple.create(s, p, ParsingUtils.generateNode(val1)));
                    }
                    case SP -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(p));
                        if (val1 != null && val2 != null)
                            triples.add(Triple.create(ParsingUtils.generateNode(val1), ParsingUtils.generateNode(val2), o));
                    }
                    case SO -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            triples.add(Triple.create(ParsingUtils.generateNode(val1), p, ParsingUtils.generateNode(val2)));
                    }
                    case PO -> {
                        val1 = binding.get(Var.alloc(p));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            triples.add(Triple.create(s, ParsingUtils.generateNode(val1), ParsingUtils.generateNode(val2)));
                    }
                    case SPO -> triples.add(t);
                }
            }
        }
        return triples;
    }

    public static List<Triple> generateTriplesFromBindings(List<Triple> template, Collection<Binding> bindings) {
        List<Triple> triples = new LinkedList<>();
        Node s, p, o;
        Node node1, node2;
        for (Binding binding : bindings) {
            for (Triple t : template) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (QueryUtils.extractVariablesPattern(s, p, o)) {
                    case S -> {
                        node1 = binding.get(Var.alloc(s));
                        if (node1 != null)
                            triples.add(Triple.create(node1, p, o));
                    }
                    case P -> {
                        node1 = binding.get(Var.alloc(p));
                        if (node1 != null)
                            triples.add(Triple.create(s, node1, o));
                    }
                    case O -> {
                        node1 = binding.get(Var.alloc(o));
                        if (node1 != null)
                            triples.add(Triple.create(s, p, node1));
                    }
                    case SP -> {
                        node1 = binding.get(Var.alloc(s));
                        node2 = binding.get(Var.alloc(p));
                        if (node1 != null && node2 != null)
                            triples.add(Triple.create(node1, node2, o));
                    }
                    case SO -> {
                        node1 = binding.get(Var.alloc(s));
                        node2 = binding.get(Var.alloc(o));
                        if (node1 != null && node2 != null)
                            triples.add(Triple.create(node1, p, node2));
                    }
                    case PO -> {
                        node1 = binding.get(Var.alloc(p));
                        node2 = binding.get(Var.alloc(o));
                        if (node1 != null && node2 != null)
                            triples.add(Triple.create(s, node1, node2));
                    }
                    case SPO -> triples.add(t);
                }
            }
        }
        return triples;
    }
}
