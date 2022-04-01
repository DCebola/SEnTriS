package pt.fct.nova.id.srv.Resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Class with control endpoints providing version information, endpoint statistics and operational metrics.
 */
@Path("/ctrl")
public class ControlResource {

    @Path("/version")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response version() {
        return Response.ok(System.getProperty("version")).build();
    }

    @Path("/stats")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response endpointStatistics() {
        return Response.ok().status(Status.NOT_IMPLEMENTED).build();
    }

    @Path("/metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response operationalMetrics() {
        return Response.ok().status(Status.NOT_IMPLEMENTED).build();
    }
}
