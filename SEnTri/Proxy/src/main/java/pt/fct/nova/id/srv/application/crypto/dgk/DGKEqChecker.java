package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

public class DGKEqChecker implements Serializable, DGK_Key {
    @Serial
    private static final long serialVersionUID = 4574519213202483629L;

    // Equality Check Key Parameters
    private final BigInteger p;
    private final BigInteger vp;
    private final BigInteger n;

    private final long u;

    public DGKEqChecker(BigInteger p, BigInteger vp, BigInteger n, long u) {
        this.p = p;
        this.vp = vp;
        this.n = n;
        this.u = u;
    }

    /**
     * Compute DGK equality, if m1 and m2 are equal then m1 - m2 = 0
     * (c1 - c2)^vp (mod p) = 1 <=> g^{vp*(m1 - m2)} (mod p) = 1
     *
     * @param ciphertext1 - DGK ciphertext1
     * @param ciphertext2 - DGK ciphertext2
     * @return boolean - true, if are equal, false otherwise.
     * @throws HomomorphicException - If the ciphertext is larger than N, an exception will be thrown
     */
    public boolean check(BigInteger ciphertext1, BigInteger ciphertext2) throws HomomorphicException {
        return NTL.POSMOD(subtract(ciphertext1, ciphertext2).modPow(vp, p), p).equals(BigInteger.ONE);
    }


    /**
     * Subtract encrypted values.
     *
     * @param ciphertext1 - Encrypted DGK value
     * @param ciphertext2 - Encrypted DGK value
     * @return DGK encrypted ciphertext with ciphertext1 - ciphertext2
     * @throws HomomorphicException - If either ciphertext is greater than N or negative, throw an exception
     */
    private BigInteger subtract(BigInteger ciphertext1, BigInteger ciphertext2) throws HomomorphicException {
        if (ciphertext1.signum() == -1 || ciphertext1.compareTo(n) > 0) {
            throw new HomomorphicException("DGKEqCheck: Invalid Parameter ciphertext1: " + ciphertext1);
        } else if (ciphertext2.signum() == -1 || ciphertext2.compareTo(n) > 0) {
            throw new HomomorphicException("DGKEqCheck: Invalid Parameter ciphertext2: " + ciphertext2);
        }
        return ciphertext1.multiply(ciphertext2.modPow(BigInteger.valueOf(u - 1), n)).mod(n);
    }
}