package pt.fct.nova.id.srv.application.clients.iam;

import pt.fct.nova.id.srv.presentation.api.dtos.Role;

import java.util.List;

public class UserAccessPolicy extends AccessPolicy {
    private Role role;

    public UserAccessPolicy(Role role, List<String> own, List<String> read, List<String> write) {
        super(own, read, write);
        this.role = role;
    }

    public UserAccessPolicy(Role basic) {
        super();
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
