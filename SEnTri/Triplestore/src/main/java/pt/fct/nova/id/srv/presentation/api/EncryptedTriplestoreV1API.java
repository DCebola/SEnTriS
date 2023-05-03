package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

public interface EncryptedTriplestoreV1API extends EncryptedTriplestoreAPI {
    @POST
    @Path("proxy/{triplestoreID}/search")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response prepareSearch(@PathParam("triplestoreID") String triplestoreID,
                           List<String> trapdoors,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
