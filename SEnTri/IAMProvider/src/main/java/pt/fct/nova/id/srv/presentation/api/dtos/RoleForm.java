package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;


public class RoleForm {

    @FormParam("issuer")
    @PartType(MediaType.TEXT_PLAIN)
    private final String issuer;

    @FormParam("role")
    @PartType(MediaType.TEXT_PLAIN)
    @DefaultValue("PRIVILEGED")
    private final Role role;

    public RoleForm(String issuer, Role role) {
        this.issuer = issuer;
        this.role = role;
    }

    public RoleForm() {
        this.issuer = null;
        this.role = null;
    }


    public String getIssuer() {
        return issuer;
    }

    public Role getRole() {
        return role;
    }
}
