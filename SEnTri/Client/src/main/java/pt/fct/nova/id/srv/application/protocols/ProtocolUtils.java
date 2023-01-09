package pt.fct.nova.id.srv.application.protocols;

import java.nio.ByteBuffer;

public class ProtocolUtils {
    public static int integerFromByteArray(byte [] bytes){
        return ByteBuffer.allocate(Integer.BYTES).put(bytes).rewind().getInt();
    }

    public static byte[] integerToByteArray(int i) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(i).array();
    }
}
