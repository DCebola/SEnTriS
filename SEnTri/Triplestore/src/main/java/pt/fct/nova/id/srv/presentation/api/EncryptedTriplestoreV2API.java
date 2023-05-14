package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.PrepareSearchV2Form;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface EncryptedTriplestoreV2API extends EncryptedTriplestoreAPI {
    @POST
    @Path("proxy/{triplestoreID}/search")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response prepareSearch(@PathParam("triplestoreID") String triplestoreID,
                           @MultipartForm PrepareSearchV2Form search,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
