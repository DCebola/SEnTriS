package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;


public interface TriplestoreAPI {

    @POST
    @Path("create/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response create(
            @PathParam("storeID") String storeID,
            @MultipartForm UploadForm form);

    @POST
    @Path("upload/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response upload(
            @PathParam("storeID") String storeID,
            @MultipartForm UploadForm form);

    @POST
    @Path("/query/{storeID}")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@PathParam("storeID") String storeID, String query);

    @DELETE
    @Path("/delete/{storeID}")
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("storeID") String storeID);

}
