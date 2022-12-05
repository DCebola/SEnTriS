package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class RequestDecisionForm {

    @FormParam("target")
    private final String target;

    @FormParam("accept")
    @DefaultValue("false")
    private final boolean accept;

    public RequestDecisionForm(String target, boolean accept) {
        this.target = target;
        this.accept = accept;
    }

    public RequestDecisionForm() {
        target = null;
        accept = false;
    }

    public String getTarget() {
        return target;
    }

    public boolean isAccept() {
        return accept;
    }
}
