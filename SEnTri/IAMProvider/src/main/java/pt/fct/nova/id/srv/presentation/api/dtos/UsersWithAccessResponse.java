package pt.fct.nova.id.srv.presentation.api.dtos;

import java.util.Set;

public record UsersWithAccessResponse(String owner, Set<String> read, Set<String> write) {

}
