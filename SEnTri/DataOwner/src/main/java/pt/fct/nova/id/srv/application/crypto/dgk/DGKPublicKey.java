package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.*;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.HashMap;

import pt.fct.nova.id.srv.application.crypto.dgk.misc.CipherConstants;
import pt.fct.nova.id.srv.application.crypto.dgk.misc.HomomorphicException;
import pt.fct.nova.id.srv.application.crypto.dgk.misc.NTL;

public final class DGKPublicKey implements DGK_Key, Serializable, PublicKey, CipherConstants {

    @Serial
    private static final long serialVersionUID = -1613333167285302035L;
    private final BigInteger n;
    private final BigInteger g;
    private final BigInteger h;
    private final long u;
    private final BigInteger bigU;
    private final HashMap<Long, BigInteger> gLUT = new HashMap<>();

    private final int l;
    private final int t;
    private final int k;
    private final int rLength;

    public DGKPublicKey(BigInteger n, BigInteger g, BigInteger h, BigInteger u, int l, int t, int k, int rLength) {
        this.n = n;
        this.g = g;
        this.h = h;
        this.u = u.longValue();
        this.bigU = u;
        this.l = l;
        this.t = t;
        this.k = k;
        this.rLength = rLength;
    }


    public void generategLUT() {
        for (long i = 0; i < u; ++i) {
            BigInteger out = g.modPow(BigInteger.valueOf(i), n);
            gLUT.put(i, out);
        }
    }

    public BigInteger ZERO() {
        return encrypt(0);
    }

    public BigInteger ONE() {
        return encrypt(1);
    }


    public String getAlgorithm() {
        return "DGK";
    }

    public String getFormat() {
        return "X.509";
    }

    public byte[] getEncoded() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public long getU() {
        return u;
    }

    public BigInteger getN() {
        return n;
    }

    public int getL() {
        return l;
    }

    public BigInteger getH() {
        return h;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getBigU() {
        return bigU;
    }

    public HashMap<Long, BigInteger> getgLUT() {
        return gLUT;
    }

    public int getT() {
        return t;
    }

    public int getK() {
        return k;
    }

    // Operations

    /**
     * Encrypt plaintext with DGK Public Key
     * Compute ciphertext = g^{m}h^{r} (mod n)
     *
     * @param plaintext - the plaintext to encrypt
     * @return - DGK ciphertext
     * throws HomomorphicException
     * - If the plaintext is larger than the plaintext supported by DGK Public Key,
     * an exception will be thrown
     */
    public BigInteger encrypt(BigInteger plaintext) {
        return encrypt(plaintext.longValue());
    }

    private BigInteger encrypt(long plaintext) {
        BigInteger ciphertext;
        if (plaintext < -1) {
            throw new IllegalArgumentException("Encryption Invalid Parameter: the plaintext is not in Zu (plaintext < 0)"
                    + " value of Plain Text is: " + plaintext);
        } else if (plaintext >= u) {
            throw new IllegalArgumentException("Encryption Invalid Parameter: the plaintext is not in Zu"
                    + " (plaintext >= U) value of Plain Text is: " + plaintext);
        }

        //first part = g^m (mod n)
        gLUT.computeIfAbsent(plaintext, p -> g.modPow(BigInteger.valueOf(p), n));


        // Generate 3 * t bit random number
        BigInteger r = NTL.generateXBitRandom(rLength * t);

        // First part = g^m
        BigInteger firstPart = gLUT.get(plaintext);
        BigInteger secondPart = h.modPow(r, n);
        ciphertext = NTL.POSMOD(firstPart.multiply(secondPart), n);
        return ciphertext;
    }

    /**
     * Add encrypted values.
     * Warning: If the sum exceeds N, it is subject to N
     *
     * @param ciphertext1 - Encrypted DGK value
     * @param ciphertext2 - Encrypted DGK value
     * @return DGK encrypted ciphertext with ciphertext1 + ciphertext2
     * @throws HomomorphicException - If either ciphertext is greater than N or negative, throw an exception
     */
    public BigInteger add(BigInteger ciphertext1, BigInteger ciphertext2)
            throws HomomorphicException {
        if (ciphertext1.signum() == -1 || ciphertext1.compareTo(n) > 0) {
            throw new HomomorphicException("DGKAdd Invalid Parameter ciphertext1: " + ciphertext1);
        } else if (ciphertext2.signum() == -1 || ciphertext2.compareTo(n) > 0) {
            throw new HomomorphicException("DGKAdd Invalid Parameter ciphertext2: " + ciphertext2);
        }
        return ciphertext1.multiply(ciphertext2).mod(n);
    }

    /**
     * Subtract encrypted values.
     *
     * @param ciphertext1 - Encrypted DGK value
     * @param ciphertext2 - Encrypted DGK value
     * @return DGK encrypted ciphertext with ciphertext1 - ciphertext2
     * @throws HomomorphicException - If either ciphertext is greater than N or negative, throw an exception
     */
    public BigInteger subtract(BigInteger ciphertext1, BigInteger ciphertext2) throws HomomorphicException {
        if (ciphertext2.signum() == -1 || ciphertext2.compareTo(n) > 0) {
            throw new HomomorphicException("DGKMultiply Invalid Parameter ciphertext: " + ciphertext2);
        }
        return add(ciphertext1, ciphertext2.modPow(BigInteger.valueOf(u - 1), n));
    }

}