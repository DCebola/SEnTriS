package pt.fct.nova.id.srv.presentation.api.dtos;

import java.util.List;
public record UserDTO(Role role, String username, String password, List<String> own, List<String> read, List<String> write) {

}
