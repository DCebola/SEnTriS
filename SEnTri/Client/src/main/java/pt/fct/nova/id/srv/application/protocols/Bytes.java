package pt.fct.nova.id.srv.application.protocols;

import java.util.Arrays;

public final class Bytes {
    private final byte[] data;

    public Bytes(byte[] data) {
        if (data == null) {
            throw new NullPointerException();
        }
        this.data = data;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Bytes)) {
            return false;
        }
        return Arrays.equals(data, ((Bytes) other).data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    public byte[] getData() {
        return data;
    }
}
