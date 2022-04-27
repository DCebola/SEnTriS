package pt.fct.nova.id.srv.presentation.api;

import jakarta.activation.MimeType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

public interface StorageAPI {
    String TEXT_TURTLE = "text/turtle ";
    String RDF_XML = "application/rdf+xml";
    String N_TRIPLES = "application/n-triples";
    String TRIG = "text/trig";
    String N_QUADS = "application/n-quads";
    String TRIX_XML = "application/trix+xml";
    String RDF_THRIFT = "application/rdf+thrift";
    String RDF_PROTOBUF = "application/rdf+protobuf";

    @POST
    @Path("upload/{storeID}")
    @Consumes({TEXT_TURTLE, RDF_XML, N_TRIPLES, TRIG, N_QUADS, TRIX_XML, RDF_THRIFT, RDF_PROTOBUF})
    @Produces(MediaType.APPLICATION_JSON)
    Response upload(
            @PathParam("storeID") String storeID,
            @DefaultValue("TTL") @QueryParam("syntax") String syntax,
            InputStream contents);

    @GET
    @Path("/download/{storeID}")
    @Produces({TEXT_TURTLE, RDF_XML, N_TRIPLES, TRIG, N_QUADS, TRIX_XML, RDF_THRIFT, RDF_PROTOBUF})
    Response download(@PathParam("storeID") String storeID, @DefaultValue("TTL") @QueryParam("syntax") String syntax);

    @GET
    @Path("/{storeID}/query/")
    @Produces(MediaType.APPLICATION_JSON)
    Response query(@PathParam("storeID") String storeID);

}
