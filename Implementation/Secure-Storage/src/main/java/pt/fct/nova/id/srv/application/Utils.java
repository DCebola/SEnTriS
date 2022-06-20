package pt.fct.nova.id.srv.application;

import org.apache.jena.graph.Node;
import pt.fct.nova.id.srv.application.query.jobs.jobs1.OrderByDirection;
import pt.fct.nova.id.srv.application.query.jobs.VariablesPattern;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class Utils {

    public static OrderByDirection extractOrderDirection(int dir){
        if (dir == 1)
            return OrderByDirection.ASCENDING;
        else if (dir == -1)
            return OrderByDirection.DESCENDING;
        else
            return null;
    }

    public static VariablesPattern extractVariablesPattern(Node subject, Node predicate, Node object) {
        if (!subject.isConcrete() && predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.S;
        else if (subject.isConcrete() && !predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.P;
        else if (subject.isConcrete() && predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.O;
        else if (!subject.isConcrete() && !predicate.isConcrete() && object.isConcrete())
            return VariablesPattern.SP;
        else if (!subject.isConcrete() && predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.SO;
        else if (subject.isConcrete() && !predicate.isConcrete() && !object.isConcrete())
            return VariablesPattern.PO;
        else
            return VariablesPattern.SPO;
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

    public static String uuidFromBase64(String str) {
        byte[] bytes = decodeBase64(str);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();
    }
}
