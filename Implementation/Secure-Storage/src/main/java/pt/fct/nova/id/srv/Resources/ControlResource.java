package pt.fct.nova.id.srv.Resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource providing endpoints with control information.
 */
@Path("/ctrl")
public class ControlResource {

    @Path("/version")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        return Response.ok(System.getProperty("deployment.version")).build();
    }
}
