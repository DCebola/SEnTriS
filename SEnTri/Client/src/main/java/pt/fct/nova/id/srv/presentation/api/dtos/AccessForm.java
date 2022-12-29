package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AccessForm {

    @FormParam("issuer")
    private final String issuer;
    @FormParam("write")
    @DefaultValue("false")
    private final boolean write;

    public AccessForm(String issuer, boolean write) {
        this.issuer = issuer;
        this.write = write;
    }

    public AccessForm() {
        this.issuer = null;
        this.write = false;
    }

    public boolean getWrite() {
        return write;
    }

    public String getIssuer() {
        return issuer;
    }
}
