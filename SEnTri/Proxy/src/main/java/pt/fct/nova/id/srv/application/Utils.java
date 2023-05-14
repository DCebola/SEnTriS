package pt.fct.nova.id.srv.application;


import java.nio.ByteBuffer;
import java.util.*;
import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class Utils {
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
