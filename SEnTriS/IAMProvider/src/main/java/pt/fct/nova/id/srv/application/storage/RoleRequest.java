package pt.fct.nova.id.srv.application.storage;

import pt.fct.nova.id.srv.presentation.dtos.Role;

public record RoleRequest(String requestID, String username, Role role) {
}
