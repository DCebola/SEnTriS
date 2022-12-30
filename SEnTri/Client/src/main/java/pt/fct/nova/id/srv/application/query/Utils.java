package pt.fct.nova.id.srv.application.query;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.graph.GraphFactory;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.SearchJob;
import pt.fct.nova.id.srv.application.query.jobs.SerializableBinding;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.nio.ByteBuffer;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.COMPOUND_KEYWORD;
import static pt.fct.nova.id.srv.application.protocols.EncryptionProtocol.KEYWORD_FORMAT;
import static pt.fct.nova.id.srv.application.query.jobs.VariablesPattern.*;

public class Utils {
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
        return uuidToBase64(UUID.randomUUID().toString());
    }

    public static String uuidToBase64(String str) {
        UUID uuid = UUID.fromString(str);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return encodeBase64URLSafeString(bb.array());
    }

    public static Graph generateGraphFromBindings(List<Triple> constructTemplate, Collection<Binding> bindings) {
        Graph g = GraphFactory.createDefaultGraph();
        Node s, p, o;
        Node n1, n2;
        for (Binding binding : bindings) {
            for (Triple t : constructTemplate) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (pt.fct.nova.id.srv.application.query.Utils.extractVariablesPattern(s, p, o)) {
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

    public static Graph generateGraphFromSerializableBindings(List<Triple> constructTemplate, Collection<SerializableBinding> bindings) {
        Graph g = GraphFactory.createDefaultGraph();
        Node s, p, o;
        String val1, val2;
        for (SerializableBinding binding : bindings) {
            for (Triple t : constructTemplate) {
                s = t.getSubject();
                p = t.getPredicate();
                o = t.getObject();
                switch (Utils.extractVariablesPattern(s, p, o)) {
                    case S -> {
                        val1 = binding.get(Var.alloc(s));
                        if (val1 != null)
                            g.add(Triple.create(NodeFactory.createURI(val1), p, o));
                    }
                    case P -> {
                        val1 = binding.get(Var.alloc(p));
                        if (val1 != null)
                            g.add(Triple.create(s, NodeFactory.createURI(val1), o));
                    }
                    case O -> {
                        val1 = binding.get(Var.alloc(o));
                        if (val1 != null)
                            g.add(Triple.create(s, p, NodeFactory.createURI(val1)));
                    }
                    case SP -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(p));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(NodeFactory.createURI(val1), NodeFactory.createURI(val2), o));
                    }
                    case SO -> {
                        val1 = binding.get(Var.alloc(s));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(NodeFactory.createURI(val1), p, NodeFactory.createURI(val2)));
                    }
                    case PO -> {
                        val1 = binding.get(Var.alloc(p));
                        val2 = binding.get(Var.alloc(o));
                        if (val1 != null && val2 != null)
                            g.add(Triple.create(s, NodeFactory.createURI(val1), NodeFactory.createURI(val2)));
                    }
                    case SPO -> g.add(t);
                }
            }
        }
        return g;
    }
}
