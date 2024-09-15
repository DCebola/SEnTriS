package pt.fct.nova.id.srv.application.crypto.dgk;

import java.math.BigInteger;

public class DGKEqUtils {

    /**
     * @param key        - DGK equality key.
     * @param ciphertext - DGK ciphertext
     * @return ciphertext.mod(n)
     */
    public static BigInteger mod(DGKEqKey key, BigInteger ciphertext) {
        return ciphertext.mod(key.getN());
    }

    /**
     * Compute DGK equality, if m1 and m2 are equal then m1 - m2 = 0
     * (c1 - c2)^vp (mod p) = 1 <=> g^{vp*(m1 - m2)} (mod p) = 1
     *
     * @param key         - DGK equality key.
     * @param ciphertext1 - DGK ciphertext1
     * @param ciphertext2 - DGK ciphertext2
     * @return boolean - true, if are equal, false otherwise.
     * @throws HomomorphicException - If the ciphertext is larger than N, an exception will be thrown
     */
    public static boolean equals(DGKEqKey key, BigInteger ciphertext1, BigInteger ciphertext2) {
        BigInteger p = key.getP();
        BigInteger vp = key.getVp();
        return NTL.POSMOD(ciphertext1.modPow(vp, p), p).subtract(NTL.POSMOD(ciphertext2.modPow(vp, p), p)).equals(BigInteger.ZERO);
    }
}