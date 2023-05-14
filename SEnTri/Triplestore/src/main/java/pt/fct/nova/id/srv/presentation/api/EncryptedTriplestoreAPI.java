package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface EncryptedTriplestoreAPI {

    @POST
    @Path("{triplestoreID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response upload(@PathParam("triplestoreID") String triplestoreID,
                    Map<String, String> encryptedNodes,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("{triplestoreID}/search")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response search(@PathParam("triplestoreID") String triplestoreID,
                    List<String> trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("triplestoreID") String triplestoreID,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/delete")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("triplestoreID") String triplestoreID,
                    List<String> trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

}
