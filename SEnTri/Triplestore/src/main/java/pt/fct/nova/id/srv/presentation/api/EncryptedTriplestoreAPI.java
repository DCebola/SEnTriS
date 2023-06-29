package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UpdateForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;

public interface EncryptedTriplestoreAPI {

    @POST
    @Path("{triplestoreID}")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(TEXT_PLAIN)
    Response upload(@PathParam("triplestoreID") String triplestoreID,
                    byte[] encryptedNodes,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("{triplestoreID}/search")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_OCTET_STREAM)
    Response search(@PathParam("triplestoreID") String triplestoreID,
                    byte[] trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("triplestoreID") String triplestoreID,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/delete")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("triplestoreID") String triplestoreID,
                    byte[] trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/update")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response update(@PathParam("triplestoreID") String triplestoreID,
                    @MultipartForm UpdateForm search,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

}
