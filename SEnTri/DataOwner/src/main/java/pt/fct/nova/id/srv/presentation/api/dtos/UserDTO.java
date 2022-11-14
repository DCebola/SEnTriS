package pt.fct.nova.id.srv.presentation.api.dtos;

import java.util.Set;

public record UserDTO(Role role, String username, String password, Set<String> own, Set<String> read, Set<String> write) {

}
