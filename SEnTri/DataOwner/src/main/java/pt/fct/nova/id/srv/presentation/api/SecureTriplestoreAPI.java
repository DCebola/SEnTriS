package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

public interface SecureTriplestoreAPI {

    @POST
    @Path("create/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(
            @PathParam("storeID") String storeID,
            @MultipartForm SecureUploadForm form) throws HttpResponseException;

    @POST
    @Path("upload/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(
            @PathParam("storeID") String storeID,
            @MultipartForm SecureUploadForm form) throws HttpResponseException;

}
