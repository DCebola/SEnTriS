package pt.fct.nova.id.srv.application.query;

import org.apache.jena.graph.Node;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;
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
