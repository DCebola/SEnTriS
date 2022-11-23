package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class RequestDecisionForm {

    @FormParam("issuer")
    private final String issuer;

    @FormParam("accept")
    @DefaultValue("false")
    private final boolean accept;

    public RequestDecisionForm(String issuer, boolean accept) {
        this.issuer = issuer;
        this.accept = accept;
    }

    public RequestDecisionForm() {
        issuer = null;
        accept = false;
    }

    public String getIssuer() {
        return issuer;
    }

    public boolean isAccept() {
        return accept;
    }
}
