package pt.fct.nova.id.srv.application;


import java.nio.ByteBuffer;
import java.util.*;


public class Utils {

    public static byte[] generateID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
