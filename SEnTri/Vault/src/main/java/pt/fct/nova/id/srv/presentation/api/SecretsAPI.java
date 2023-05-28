package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface SecretsAPI {

    @POST
    @Path("/{triplestoreID}")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(TEXT_PLAIN)
    Response createSecrets(@PathParam("triplestoreID") String triplestoreID,
                           byte[] secrets,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{triplestoreID}")
    @Produces(APPLICATION_OCTET_STREAM)
    Response getSecrets(@PathParam("triplestoreID") String triplestoreID,
                        @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response deleteSecrets(@PathParam("triplestoreID") String triplestoreID,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
