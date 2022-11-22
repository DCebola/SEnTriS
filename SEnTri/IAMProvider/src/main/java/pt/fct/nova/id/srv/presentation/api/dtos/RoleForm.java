package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;


public class RoleForm {

    @FormParam("issuer")
    private final String issuer;

    @FormParam("role")
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
