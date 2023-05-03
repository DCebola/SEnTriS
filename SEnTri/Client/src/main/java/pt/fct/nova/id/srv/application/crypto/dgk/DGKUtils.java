package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

public class DGKUtils {
    private static DGKKeyPairGenerator keyPairGenerator;
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

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

    public static BigInteger generateMask(BigInteger r, DGKPublicKey k, boolean negate) {
        if (negate)
            return k.getH().modPow(r.negate(), k.getN());
        return k.getH().modPow(r, k.getN());
    }

    public static BigInteger unmask(DGKPublicKey k, BigInteger r, BigInteger ciphertext) throws HomomorphicException {
        return k.reencrypt(r, ciphertext);
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
