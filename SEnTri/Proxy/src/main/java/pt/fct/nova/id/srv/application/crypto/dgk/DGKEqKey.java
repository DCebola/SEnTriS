package pt.fct.nova.id.srv.application.crypto.dgk;

import java.math.BigInteger;

public class DGKEqKey implements DGK_Key {
    private final BigInteger p;
    private final BigInteger vp;
    private final BigInteger n;
    private final long u;

    public DGKEqKey(BigInteger p, BigInteger vp, BigInteger n, long u) {
        this.p = p;
        this.vp = vp;
        this.n = n;
        this.u = u;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getVp() {
        return vp;
    }

    public BigInteger getN() {
        return n;
    }

    public long getU() {
        return u;
    }
}
