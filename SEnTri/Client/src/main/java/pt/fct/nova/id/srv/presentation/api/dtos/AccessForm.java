package pt.fct.nova.id.srv.presentation.api.dtos;

import jakarta.ws.rs.DefaultValue;
import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class AccessForm {

    @FormParam("user")
    private final String user;
    @FormParam("write")
    @DefaultValue("false")
    private final boolean write;

    public AccessForm(String user, boolean write) {
        this.user = user;
        this.write = write;
    }

    public AccessForm() {
        this.user = null;
        this.write = false;
    }

    public boolean getWrite() {
        return write;
    }

    public String getUser() {
        return user;
    }
}
