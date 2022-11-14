package pt.fct.nova.id.srv.application.clients.iam;

import java.util.LinkedList;
import java.util.List;

public class AccessPolicy {

    private final List<String> own;
    private final List<String> read;
    private final List<String> write;

    public AccessPolicy(List<String> own, List<String> read, List<String> write) {
        this.own = own;
        this.read = read;
        this.write = write;
    }

    public AccessPolicy() {
        this.own = new LinkedList<>();
        this.read = new LinkedList<>();
        this.write = new LinkedList<>();
    }

    public List<String> getOwn() {
        return own;
    }

    public List<String> getRead() {
        return read;
    }

    public List<String> getWrite() {
        return write;
    }
}
