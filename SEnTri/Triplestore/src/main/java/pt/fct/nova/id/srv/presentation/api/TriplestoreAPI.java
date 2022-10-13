package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;


public interface TriplestoreAPI {


    @POST
    @Path("create/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(
            @PathParam("storeID") String storeID,
            @MultipartForm UploadForm form) throws HttpResponseException;

    @POST
    @Path("upload/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(
            @PathParam("storeID") String storeID,
            @MultipartForm UploadForm form) throws HttpResponseException;


    @GET
    @Path("/download/{storeID}")
    @Produces({TEXT_TURTLE,
            RDF_XML,
            N_TRIPLES,
            TRIG,
            N_QUADS,
            TRIX_XML,
            RDF_THRIFT,
            RDF_PROTOBUF})
    Response download(@PathParam("storeID") String storeID, @DefaultValue("TTL") @QueryParam("syntax") String syntax);

    @GET
    @Path("/{storeID}/query/")
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@PathParam("storeID") String storeID, String query);

}
