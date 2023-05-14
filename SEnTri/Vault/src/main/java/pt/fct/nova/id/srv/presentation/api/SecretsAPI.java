package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface SecretsAPI {

    @POST
    @Path("/{triplestoreID}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response createSecrets(@PathParam("triplestoreID") String triplestoreID,
                           Map<String, String> secrets,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{triplestoreID}")
    @Produces(APPLICATION_JSON)
    Response getSecrets(@PathParam("triplestoreID") String triplestoreID,
                        @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response deleteSecrets(@PathParam("triplestoreID") String triplestoreID,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
