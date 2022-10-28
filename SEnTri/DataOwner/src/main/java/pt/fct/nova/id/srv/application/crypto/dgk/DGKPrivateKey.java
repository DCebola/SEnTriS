package pt.fct.nova.id.srv.application.crypto.dgk;

import java.io.*;
import java.math.BigInteger;
import java.security.PrivateKey;

public final class DGKPrivateKey implements DGK_Key, Serializable, PrivateKey {
    @Serial
    private static final long serialVersionUID = 4574519230502483629L;

    // Private Key Parameters
    private final BigInteger p;
    private final BigInteger q;
    private final BigInteger vp;
    private final BigInteger vq;

    public DGKPrivateKey(BigInteger p, BigInteger q, BigInteger vp,
                         BigInteger vq) {
        this.p = p;
        this.q = q;
        this.vp = vp;
        this.vq = vq;
    }

    @Serial
    private void readObject(ObjectInputStream aInputStream)
            throws ClassNotFoundException, IOException {
        aInputStream.defaultReadObject();
    }

    @Serial
    private void writeObject(ObjectOutputStream aOutputStream) throws IOException {
        aOutputStream.defaultWriteObject();
    }

    public String getAlgorithm() {
        return "DGK";
    }

    public String getFormat() {
        return "PKCS#8";
    }

    public byte[] getEncoded() {
        return null;
    }


    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getVp() {
        return vp;
    }

    public BigInteger getVq() {
        return vq;
    }

}