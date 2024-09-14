package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;

public class DGKUtils {
    private static DGKKeyPairGenerator keyPairGenerator;
    private static final SecureRandom rnd = new SecureRandom();

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

    public static BigInteger generateMask(DGKPublicKey k) {
        return k.encrypt(rnd.nextLong(0, k.getU()));
    }

    public static BigInteger unmask(DGKPublicKey k, BigInteger mask, BigInteger ciphertext) throws HomomorphicException {
        return k.subtract(ciphertext, mask);
    }

    public static KeyPair parseKeyPair(byte[] contents) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(contents);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            return (KeyPair) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


}
