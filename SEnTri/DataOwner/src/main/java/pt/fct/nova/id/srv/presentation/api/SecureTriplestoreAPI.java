package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.application.protocols.ProtocolVersion;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_JSON_RESULTS;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_QUERY;

public interface SecureTriplestoreAPI {

    @POST
    @Path("/{protocolVersion}/create/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(
            @PathParam("protocolVersion") ProtocolVersion protocolVersion,
            @PathParam("storeID") String storeID,
            @MultipartForm SecureUploadForm form);

    @POST
    @Path("/{protocolVersion}/upload/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(
            @PathParam("protocolVersion") ProtocolVersion protocolVersion,
            @PathParam("storeID") String storeID,
            @MultipartForm SecureUploadForm form);

    @POST
    @Path("/{protocolVersion}/query/{storeID}")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(
            @PathParam("protocolVersion") ProtocolVersion protocolVersion,
            @PathParam("storeID") String storeID,
            String query);

}
