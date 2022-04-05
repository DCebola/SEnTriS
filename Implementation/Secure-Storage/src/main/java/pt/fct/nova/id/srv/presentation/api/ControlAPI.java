package pt.fct.nova.id.srv.presentation.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface ControlAPI {

    @Path("/version")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    Response version();
}
