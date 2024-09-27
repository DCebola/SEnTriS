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

    public static BigInteger generateMask(DGKPublicKey pub, DGKPrivateKey priv) {
        BigInteger n = pub.getN();
        BigInteger vp = priv.getVp();
        BigInteger p = priv.getP();
        BigInteger vq = priv.getVp();
        BigInteger q = priv.getQ();
        BigInteger u = pub.getBigU();
        BigInteger r;
        while (true) {
            //Generate n bit random number
            r = NTL.generateXBitRandom(n.bitCount()).mod(n);
            if (r.equals(BigInteger.ONE) || r.equals(BigInteger.ZERO)) {
                continue;
            }

            if (r.modPow(vp, p).equals(BigInteger.ONE)) {
                continue;//h^{vp}(mod p) = 1
            }

            if (r.modPow(vq, q).equals(BigInteger.ONE)) {
                continue;//h^{vq}(mod q) = 1
            }

            if (r.modPow(vp, n).equals(BigInteger.ONE)) {
                continue;//r^{vp} (mod n) = 1
            }

            if (r.modPow(vq, n).equals(BigInteger.ONE)) {
                continue;//r^{vq} (mod n) = 1
            }

            if (r.modPow(vp.multiply(vq), n).equals(BigInteger.ONE)) {
                continue;//r^{vq*vq} (mod n) = 1
            }

            if (r.modPow(vp.multiply(vq).multiply(u), n).equals(BigInteger.ONE)) {
                continue;//r^{u*vq*vq} (mod n) = 1
            }

            if (r.gcd(n).equals(BigInteger.ONE)) {
                break;//(r, n) = 1
            }
        }
        return r;
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
