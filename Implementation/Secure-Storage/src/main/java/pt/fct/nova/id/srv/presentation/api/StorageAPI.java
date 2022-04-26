package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

public interface StorageAPI {

    @POST
    @Path("upload/{id}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    Response upload(
            @PathParam("id") String id,
            @DefaultValue("TTL") @QueryParam("syntax") String syntax,
            InputStream contents);


    @GET
    @Path("/{id}/query/")
    @Produces(MediaType.APPLICATION_JSON)
    Response query(@PathParam("id") String id);

}
