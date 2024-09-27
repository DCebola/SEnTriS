package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

public class DGKEqKey implements DGK_Key, Serializable {
    @Serial
    private static final long serialVersionUID = 2345655035642188990L;
    private final BigInteger p;
    private final BigInteger vp;

    public DGKEqKey(BigInteger p, BigInteger vp, BigInteger n, long u) {
        this.p = p;
        this.vp = vp;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getVp() {
        return vp;
    }
}
