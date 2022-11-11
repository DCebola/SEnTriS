package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class IAMRequestForm {

    @FormParam("username")
    private final String username;

    @FormParam("rolesToGrant")
    private final Role[] rolesToGrant;

    @FormParam("rolesToRevoke")
    private final Role[] rolesToRevoke;

    @FormParam("accessesToGrant")
    private final String[] accessesToGrant;

    @FormParam("accessesToRevoke")
    private final String[] accessesToRevoke;

    public IAMRequestForm(String username, Role[] rolesToGrant, Role[] rolesToRevoke, String[] accessesToGrant, String[] accessesToRevoke) {
        this.username = username;
        this.rolesToGrant = rolesToGrant;
        this.rolesToRevoke = rolesToRevoke;
        this.accessesToGrant = accessesToGrant;
        this.accessesToRevoke = accessesToRevoke;
    }

    public String getUsername() {
        return username;
    }

    public Role[] getRolesToGrant() {
        return rolesToGrant;
    }

    public Role[] getRolesToRevoke() {
        return rolesToRevoke;
    }

    public String[] getAccessesToGrant() {
        return accessesToGrant;
    }

    public String[] getAccessesToRevoke() {
        return accessesToRevoke;
    }
}
