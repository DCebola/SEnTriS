package pt.fct.nova.id.srv.application.crypto.dgk;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.SecureRandom;

import pt.fct.nova.id.srv.application.crypto.dgk.misc.CipherConstants;
import pt.fct.nova.id.srv.application.crypto.dgk.misc.HomomorphicException;
import pt.fct.nova.id.srv.application.crypto.dgk.misc.NTL;

public final class DGKKeyPairGenerator extends KeyPairGeneratorSpi implements CipherConstants {
    // Default parameters
    private int l = 16;
    private int t = 160;
    private int k = 2048;

    private int rLength = 2; // h^r such that r >= 2*t bits
    private SecureRandom rnd = null;

    public DGKKeyPairGenerator() {
        initialize(k, null);
    }

    /**
     * Initialize DGK Key pair generator and sets DGK parameters
     *
     * @param l - sets size of plaintext
     * @param t - security parameter
     * @param k - number of bits of keys
     * @throws HomomorphicException     - DGK parameters exception
     * @throws IllegalArgumentException - key size exception
     */
    public DGKKeyPairGenerator(int l, int t, int k, int rLength) throws HomomorphicException {
        if (l < 0 || l > 32) {
            throw new HomomorphicException("DGK Keygen Invalid parameters: plaintext space must be less than 32 bits");
        }

        if (l > t || t > k) {
            throw new HomomorphicException("DGK Keygen Invalid parameters: we must have l < k < t");
        }

        if (k / 2 < t + l + 1) {
            throw new HomomorphicException("DGK Keygen Invalid parameters: we must have k > k/2 < t + l");
        }

        if (t % 2 != 0) {
            throw new HomomorphicException("DGK Keygen Invalid parameters: t must be divisible by 2 ");
        }
        if (rLength < 2) {
            throw new HomomorphicException("DGK h exponent bit length at least 2*l!");
        }
        if (k % 2 != 0) {
            throw new IllegalArgumentException("Require even number of bits!");
        }
        if (k < 1024) {
            throw new IllegalArgumentException("Minimum strength of 1024 bits required!");
        }


        this.l = l;
        this.t = t;
        this.k = k;
        initialize(k, null);
    }

    public void initialize(int k, SecureRandom random) {
        this.k = k;
        rnd = random;
    }

    /**
     * @return DGK Key Pair
     */
    public KeyPair generateKeyPair() {
        if (rnd == null) {
            rnd = new SecureRandom();
        }

        DGKPublicKey pubKey;
        DGKPrivateKey privkey;

        BigInteger p, rp;
        BigInteger q, rq;
        BigInteger g, h;
        BigInteger n, r;
        BigInteger u = TWO.pow(l);
        BigInteger vp, vq, vpvq, tmp;

        while (true) {
            //Following the instruction as stated on DGK C++ counterpart
            u = u.nextProbablePrime();
            vp = new BigInteger(t, CERTAINTY, rnd);//(160, 40, random)
            vq = new BigInteger(t, CERTAINTY, rnd);//(160, 40, random)
            vpvq = vp.multiply(vq);
            tmp = u.multiply(vp);

            int needed_bits = k / 2 - (tmp.bitLength());

            // Generate rp until p is prime such that u * vp divides p-1
            do {
                rp = new BigInteger(needed_bits, rnd);
                rp = rp.setBit(needed_bits - 1);

                /*
                 * p = rp * u * vp + 1
                 * u | p - 1
                 * vp | p - 1
                 */
                p = rp.multiply(tmp).add(BigInteger.ONE);
            }
            while (!p.isProbablePrime(CERTAINTY));

            tmp = u.multiply(vq);
            needed_bits = k / 2 - (tmp.bitLength());
            do {
                // Same method for q than for p
                rq = new BigInteger(needed_bits, rnd);
                rq = rq.setBit(needed_bits - 1);
                q = rq.multiply(tmp).add(BigInteger.ONE); // q = rq*(vq*u) + 1

                /*
                 * q - 1 | rq * vq * u
                 * Therefore,
                 * c^{vp} = g^{vp*m} (mod n) because
                 * rq | (q - 1)
                 */
            }
            while (!q.isProbablePrime(CERTAINTY));
            //Thus we ensure that q is a prime, with p-1 divides the prime numbers vq and u
            if (!NTL.POSMOD(rq, u).equals(BigInteger.ZERO) &&
                    !NTL.POSMOD(rp, u).equals(BigInteger.ZERO)) {
                break;
            }

        }

        n = p.multiply(q);
        tmp = rp.multiply(rq).multiply(u);

        while (true) {
            //Generate n bit random number
            r = NTL.generateXBitRandom(n.bitLength());
            h = r.modPow(tmp, n); // h = r^{rp*rq*u} (mod n)

            if (h.equals(BigInteger.ONE)) {
                continue;
            }

            if (h.modPow(vp, n).equals(BigInteger.ONE)) {
                continue;//h^{vp}(mod n) = 1
            }

            if (h.modPow(vq, n).equals(BigInteger.ONE)) {
                continue;//h^{vq}(mod n) = 1
            }

            if (h.modPow(u, n).equals(BigInteger.ONE)) {
                continue;//h^{u}(mod n) = 1
            }

            if (h.modPow(u.multiply(vq), n).equals(BigInteger.ONE)) {
                continue;//h^{u*vq} (mod n) = 1
            }

            if (h.modPow(u.multiply(vp), n).equals(BigInteger.ONE)) {
                continue;//h^{u*vp} (mod n) = 1
            }

            if (h.gcd(n).equals(BigInteger.ONE)) {
                break;//(h, n) = 1
            }
        }

        BigInteger rprq = rp.multiply(rq);

        while (true) {
            r = NTL.generateXBitRandom(n.bitLength());
            g = r.modPow(rprq, n); //g = r^{rp*rq}(mod n)

            if (g.equals(BigInteger.ONE)) {
                continue;// g = 1
            }

            if (!g.gcd(n).equals(BigInteger.ONE)) {
                continue;//(g, n) must be relatively prime
            }
            // h can still be of order u, vp, vq , or a combination of them different that u, vp, vq
            if (g.modPow(u, n).equals(BigInteger.ONE)) {
                continue;//g^{u} (mod n) = 1
            }
            if (g.modPow(u.multiply(u), n).equals(BigInteger.ONE)) {
                continue;//g^{u*u} (mod n) = 1
            }
            if (g.modPow(u.multiply(u).multiply(vp), n).equals(BigInteger.ONE)) {
                continue;//g^{u*u*vp} (mod n) = 1
            }

            if (g.modPow(u.multiply(u).multiply(vq), n).equals(BigInteger.ONE)) {
                continue;//g^{u*u*vp} (mod n) = 1
            }

            if (g.modPow(vp, n).equals(BigInteger.ONE)) {
                continue;//g^{vp} (mod n) = 1
            }

            if (g.modPow(vq, n).equals(BigInteger.ONE)) {
                continue;//g^{vq} (mod n) = 1
            }

            if (g.modPow(u.multiply(vq), n).equals(BigInteger.ONE)) {
                continue;//g^{u*vq}(mod n) = 1
            }

            if (g.modPow(u.multiply(vp), n).equals(BigInteger.ONE)) {
                continue;//g^{u*vp} (mod n) = 1
            }

            if (g.modPow(vpvq, n).equals(BigInteger.ONE)) {
                continue;//g^{vp*vq} (mod n) == 1
            }

            if (NTL.POSMOD(g, p).modPow(vp, p).equals(BigInteger.ONE)) {
                continue; //g^{vp} (mod p) == 1
            }

            if ((NTL.POSMOD(g, p).modPow(u, p).equals(BigInteger.ONE))) {
                continue;//g^{u} (mod p) = 1
            }

            if (NTL.POSMOD(g, q).modPow(vq, q).equals(BigInteger.ONE)) {
                continue;//g^{vq}(mod q) == 1
            }

            if ((NTL.POSMOD(g, q).modPow(u, q).equals(BigInteger.ONE))) {
                continue;//g^{u}(mod q)
            }
            break;
        }

        pubKey = new DGKPublicKey(n, g, h, u, l, t, k, rLength);
        privkey = new DGKPrivateKey(p, q, vp, vq);
        return new KeyPair(pubKey, privkey);
    }

}
