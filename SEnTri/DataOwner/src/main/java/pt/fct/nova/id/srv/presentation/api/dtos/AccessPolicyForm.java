package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AccessPolicyForm {

    @FormParam("own")
    private final String[] own;

    @FormParam("write")
    private final String[] write;

    @FormParam("read")
    private final String[] read;

    public AccessPolicyForm(String[] own, String[] write, String[] read) {
        this.own = own;
        this.write = write;
        this.read = read;
    }

    public String[] getOwn() {
        return own;
    }

    public String[] getWrite() {
        return write;
    }

    public String[] getRead() {
        return read;
    }
}
