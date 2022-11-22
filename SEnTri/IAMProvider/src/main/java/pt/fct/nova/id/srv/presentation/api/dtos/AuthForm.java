package pt.fct.nova.id.srv.presentation.api.dtos;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AuthForm {
    @FormParam("username")
    private final String username;

    @FormParam("password")
    private final String password;

    public AuthForm(String syntax, String password) {
        this.username = syntax;
        this.password = password;
    }

    public AuthForm() {
        username = null;
        password = null;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
