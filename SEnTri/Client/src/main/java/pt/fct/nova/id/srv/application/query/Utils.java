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
        Map<Var, String> res = new HashMap<>();
        switch (pattern) {
            case S -> {
                p = ParsingUtils.parseKeyword(predicate);
                o = ParsingUtils.parseKeyword(object);
                res.put(Var.alloc(subject), String.format(KEYWORD_FORMAT, S, String.format(COMPOUND_KEYWORD, p, o)));
            }
            case P -> {
                s = ParsingUtils.parseKeyword(subject);
                o = ParsingUtils.parseKeyword(object);
                res.put(Var.alloc(predicate), String.format(KEYWORD_FORMAT, P, String.format(COMPOUND_KEYWORD, s, o)));
            }
            case O -> {
                s = ParsingUtils.parseKeyword(subject);
                p = ParsingUtils.parseKeyword(predicate);
                res.put(Var.alloc(object), String.format(KEYWORD_FORMAT, O, String.format(COMPOUND_KEYWORD, s, p)));
            }
            case SP -> {
                o = ParsingUtils.parseKeyword(object);
                res.put(Var.alloc(subject), String.format(KEYWORD_FORMAT, S, o));
                res.put(Var.alloc(predicate), String.format(KEYWORD_FORMAT, P, o));
            }
            case SO -> {
                p = ParsingUtils.parseKeyword(predicate);
                res.put(Var.alloc(subject), String.format(KEYWORD_FORMAT, S, p));
                res.put(Var.alloc(object), String.format(KEYWORD_FORMAT, O, p));
            }
            case PO -> {
                s = ParsingUtils.parseKeyword(subject);
                res.put(Var.alloc(predicate), String.format(KEYWORD_FORMAT, P, s));
                res.put(Var.alloc(object), String.format(KEYWORD_FORMAT, O, s));
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
