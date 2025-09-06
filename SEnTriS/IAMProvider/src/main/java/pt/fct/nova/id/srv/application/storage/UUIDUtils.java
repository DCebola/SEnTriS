package pt.fct.nova.id.srv.application.storage;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class UUIDUtils {
    public static String generateID() {
        return uuidToBase64(UUID.randomUUID().toString());
    }

    public static String uuidToBase64(String str) {
        UUID uuid = UUID.fromString(str);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().encodeToString(bb.array());
    }
}
