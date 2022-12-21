package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureSPARQLQueryForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface QueriesAPI {

    @POST
    @Path("/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response answerSPARQLQuery(@MultipartForm SecureSPARQLQueryForm form,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/prepare")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_OCTET_STREAM)
    Response saveSearchResults(List<String> encryptedNodes,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
