package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

public interface TriplestoreAPI {


    @POST
    @Path("upload/{storeID}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Response upload(
            @PathParam("storeID") String storeID,
            @MultipartForm UploadForm form) throws HttpResponseException;

    @GET
    @Path("/download/{storeID}")
    @Produces({RDFMediaType.TEXT_TURTLE,
            RDFMediaType.RDF_XML,
            RDFMediaType.N_TRIPLES,
            RDFMediaType.TRIG,
            RDFMediaType.N_QUADS,
            RDFMediaType.TRIX_XML,
            RDFMediaType.RDF_THRIFT,
            RDFMediaType.RDF_PROTOBUF})
    Response download(@PathParam("storeID") String storeID, @DefaultValue("TTL") @QueryParam("syntax") String syntax);

    @GET
    @Path("/{storeID}/query/")
    @Produces(MediaType.APPLICATION_JSON)
    Response query(@PathParam("storeID") String storeID);

}
