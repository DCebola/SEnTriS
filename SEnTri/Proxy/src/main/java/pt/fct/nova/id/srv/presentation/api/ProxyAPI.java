package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface ProxyAPI {

    @POST
    @Path("/")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    Response answerSPARQLQuery(byte[] queryExecutionPlan,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/bindings")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response saveBinding(List<String> encryptedNodes,
                         @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
