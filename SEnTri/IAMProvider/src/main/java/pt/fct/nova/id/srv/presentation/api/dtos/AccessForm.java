package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AccessForm {

    @FormParam("issuer")
    private final String issuer;

    @FormParam("store")
    private final String storeID;

    @FormParam("write")
    private final boolean write;

    @FormParam("read")
    private final boolean read;

    public AccessForm(String issuer, String storeID, boolean write, boolean read) {
        this.issuer = issuer;
        this.storeID = storeID;
        this.write = write;
        this.read = read;
    }

    public AccessForm() {
        this.issuer = null;
        this.storeID = null;
        this.write = false;
        this.read = false;
    }

    public boolean getWrite() {
        return write;
    }

    public boolean getRead() {
        return read;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getStoreID() {
        return storeID;
    }
}
