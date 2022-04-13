package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public interface StorageAPI {

    @POST
    @Path("upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response upload();

    @GET
    @Path("query/{protocol}")
    @Produces(MediaType.APPLICATION_JSON)
    Response query(@PathParam("protocol") String protocol, @QueryParam("body") String body);

}
