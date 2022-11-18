package pt.fct.nova.id.srv.presentation.api.dtos;

import java.util.List;
public record User(Role role, String username, String password, List<String> ownedStores) {

}
