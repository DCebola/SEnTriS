package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.SecretsForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface SecretsAPI {

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response createSecrets(@Form SecretsForm secrets,
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
