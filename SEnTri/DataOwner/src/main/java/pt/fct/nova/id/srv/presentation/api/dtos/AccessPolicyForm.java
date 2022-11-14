package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import java.util.List;

public class AccessPolicyForm {

    @FormParam("own")
    private final List<String> own;

    @FormParam("write")
    private final List<String> write;

    @FormParam("read")
    private final List<String> read;

    public AccessPolicyForm(List<String> own, List<String> write, List<String> read) {
        this.own = own;
        this.write = write;
        this.read = read;
    }

    public List<String> getOwn() {
        return own;
    }

    public List<String> getWrite() {
        return write;
    }

    public List<String> getRead() {
        return read;
    }
}
