package pt.fct.nova.id.srv.presentation.dtos;

import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;


public class AuthForm {
    @FormParam("username")
    @PartType(MediaType.TEXT_PLAIN)
    private final String username;

    @FormParam("password")
    @PartType(MediaType.TEXT_PLAIN)
    private final String password;

    public AuthForm(String username, String password) {
        this.username = username;
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
