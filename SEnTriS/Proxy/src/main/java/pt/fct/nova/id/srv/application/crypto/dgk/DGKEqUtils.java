package pt.fct.nova.id.srv.application.crypto.dgk;

import java.math.BigInteger;

public class DGKEqUtils {

    /**
     *
     * @param key         - DGK equality key.
     * @param ciphertext1 - DGK ciphertext1
     * @param ciphertext2 - DGK ciphertext2
     * @return boolean - true, if are equal, false otherwise.
     */
    public static boolean equals(DGKEqKey key, BigInteger ciphertext1, BigInteger ciphertext2) {
        BigInteger p = key.getP();
        return ciphertext1.modPow(key.getVp(), p).equals(ciphertext2.modPow(key.getVp(), p));
    }

    /**
     *
     * @param key        - DGK equality key.
     * @param ciphertext - DGK ciphertext.
     * @return BigInteger - the deterministic layer of the ciphertext.
     */
    public static byte[] removeRNDLayer(DGKEqKey key, BigInteger ciphertext) {
        return ciphertext.modPow(key.getVp(), key.getP()).toByteArray();
    }

}