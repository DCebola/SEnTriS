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
    public static boolean equals(DGKEqKey key, BigInteger ciphertext1, BigInteger ciphertext2) throws HomomorphicException {
        BigInteger p = key.getP();
        return NTL.POSMOD(subtract(key, ciphertext1, ciphertext2).modPow(key.getVp(), p), p).equals(BigInteger.ONE);
    }


    /**
     * Subtract encrypted values.
     *
     * @param key         - DGK equality key.
     * @param ciphertext1 - Encrypted DGK value
     * @param ciphertext2 - Encrypted DGK value
     * @return DGK encrypted ciphertext with ciphertext1 - ciphertext2
     * @throws HomomorphicException - If either ciphertext is greater than N or negative, throw an exception
     */
    private static BigInteger subtract(DGKEqKey key, BigInteger ciphertext1, BigInteger ciphertext2) throws HomomorphicException {
        BigInteger n = key.getN();
        if (ciphertext1.signum() == -1 || ciphertext1.compareTo(key.getN()) > 0) {
            throw new HomomorphicException("DGKEqCheck: Invalid Parameter ciphertext1: " + ciphertext1);
        } else if (ciphertext2.signum() == -1 || ciphertext2.compareTo(key.getN()) > 0) {
            throw new HomomorphicException("DGKEqCheck: Invalid Parameter ciphertext2: " + ciphertext2);
        }
        long u = key.getU();
        return  ciphertext1.multiply(ciphertext2.modPow(BigInteger.valueOf(u - 1), n)).mod(n);
    }

}