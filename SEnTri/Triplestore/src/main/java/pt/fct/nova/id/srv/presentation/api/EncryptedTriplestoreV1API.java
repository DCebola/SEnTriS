package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface EncryptedTriplestoreV1API extends EncryptedTriplestoreAPI {
    @POST
    @Path("proxy/{triplestoreID}/search")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(TEXT_PLAIN)
    Response prepareSearch(@PathParam("triplestoreID") String triplestoreID,
                           byte[] trapdoors,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
