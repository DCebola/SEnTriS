package pt.fct.nova.id.srv.presentation.apis;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public interface ControlAPI {

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    Response version();
}
