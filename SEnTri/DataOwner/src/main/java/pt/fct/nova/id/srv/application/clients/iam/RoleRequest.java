package pt.fct.nova.id.srv.application.clients.iam;

import pt.fct.nova.id.srv.presentation.api.dtos.Role;

public record RoleRequest(String target, Role role) {
}
