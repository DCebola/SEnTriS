package pt.fct.nova.id.srv.application.query;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.jobs.SearchJob;
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

    public static Map<Var, String> generateKeywordMap(Node subject, Node predicate, Node object, VariablesPattern pattern) throws InvalidNodeException {
        String s, p, o;
        s = ParsingUtils.parseNodeIRI(subject);
        p = ParsingUtils.parseNodeIRI(predicate);
        o = ParsingUtils.parseNodeIRI(object);
        Map<Var, String> res = new HashMap<>();
        switch (pattern) {
            case S -> res.put(Var.alloc(s), String.format(KEYWORD_FORMAT, PO, String.format(COMPOUND_KEYWORD, p, o)));
            case P -> res.put(Var.alloc(p), String.format(KEYWORD_FORMAT, SO, String.format(COMPOUND_KEYWORD, s, o)));
            case O -> res.put(Var.alloc(o), String.format(KEYWORD_FORMAT, SP, String.format(COMPOUND_KEYWORD, s, p)));
            case SP -> {
                res.put(Var.alloc(s), String.format(KEYWORD_FORMAT, O, o));
                res.put(Var.alloc(p), String.format(KEYWORD_FORMAT, O, o));
            }
            case SO -> {
                res.put(Var.alloc(s), String.format(KEYWORD_FORMAT, P, p));
                res.put(Var.alloc(o), String.format(KEYWORD_FORMAT, P, p));
            }
            case PO -> {
                res.put(Var.alloc(p), String.format(KEYWORD_FORMAT, S, s));
                res.put(Var.alloc(o), String.format(KEYWORD_FORMAT, S, s));
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

}
