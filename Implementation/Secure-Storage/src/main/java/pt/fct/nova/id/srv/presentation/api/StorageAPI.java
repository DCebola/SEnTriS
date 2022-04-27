package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

public interface StorageAPI {

    @POST
    @Path("upload/{storeID}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    Response upload(
            @PathParam("storeID") String storeID,
            @DefaultValue("TTL") @QueryParam("syntax") String syntax,
            InputStream contents);

    @GET
    @Path("/download/{storeID}")
    @Produces(MediaType.APPLICATION_JSON)
    Response download(@PathParam("storeID") String storeID, @DefaultValue("TTL") @QueryParam("syntax") String syntax);

    @GET
    @Path("/{storeID}/query/")
    @Produces(MediaType.APPLICATION_JSON)
    Response query(@PathParam("storeID") String storeID);

}
