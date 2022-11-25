package pt.fct.nova.id.srv.application;

import pt.fct.nova.id.srv.presentation.api.dtos.Role;

public record RoleRequest(String user, Role role) {
}
