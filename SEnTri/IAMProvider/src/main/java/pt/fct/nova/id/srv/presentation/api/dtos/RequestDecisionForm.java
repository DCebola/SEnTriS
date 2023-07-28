package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class RequestDecisionForm {

    @FormParam("target")
    @PartType(MediaType.TEXT_PLAIN)
    private final String target;

    @FormParam("accept")
    @PartType(MediaType.TEXT_PLAIN)
    @DefaultValue("false")
    private boolean accept;

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
