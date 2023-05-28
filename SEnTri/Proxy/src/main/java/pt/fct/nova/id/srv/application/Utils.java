package pt.fct.nova.id.srv.application;


import java.nio.ByteBuffer;
import java.util.*;

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafe;

public class Utils {

    public static byte[] generateID() {
        return uuidToBase64(UUID.randomUUID());
    }

    public static byte[] uuidToBase64(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return encodeBase64URLSafe(bb.array());
    }

}
