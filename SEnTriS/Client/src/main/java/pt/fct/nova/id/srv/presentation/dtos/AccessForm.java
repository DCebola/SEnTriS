package pt.fct.nova.id.srv.presentation.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class AccessForm {

    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;
    @FormParam("write")
    @DefaultValue("false")
    @PartType(MediaType.TEXT_PLAIN)
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
