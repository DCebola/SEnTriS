package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyPair;

public class DGKUtils {
    private static DGKKeyPairGenerator keyPairGenerator;

    public static KeyPair generateKeyPair() throws HomomorphicException {
        if (keyPairGenerator == null)
            keyPairGenerator = new DGKKeyPairGenerator(
                    Integer.parseInt(System.getenv("DGK_L")),
                    Integer.parseInt(System.getenv("DGK_T")),
                    Integer.parseInt(System.getenv("DGK_K")),
                    Integer.parseInt(System.getenv("DGK_R"))
            );
        return keyPairGenerator.generateKeyPair();
    }

    public static BigInteger generateRandom(DGKPublicKey k) {
        return NTL.generateXBitRandom(3 * k.getT());
    }

    public static KeyPair parseKeyPair(byte[] contents) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(contents);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (KeyPair) ois.readObject();
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }
}
