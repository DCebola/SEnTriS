package pt.fct.nova.id.srv.application.clients.iam;

import pt.fct.nova.id.srv.presentation.api.dtos.Role;

import java.util.LinkedList;
import java.util.List;

public class UserData {
    private Role role;
    private final List<String> owned;

    public UserData(Role role, List<String> owned) {
        this.role = role;
        this.owned = owned;
    }

    public UserData(Role role) {
        this.role = role;
        this.owned = new LinkedList<>();
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<String> getOwned() {
        return owned;
    }
}
