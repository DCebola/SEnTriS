package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;


public interface TriplestoreAPI {

    @POST
    @Path("/{triplestoreID}")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(TEXT_PLAIN)
    Response upload(@PathParam("triplestoreID") String triplestoreID,
                    byte[] triples,
                    @DefaultValue("false") @QueryParam("isSchema") boolean isSchema,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/query/{triplestoreID}")
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@PathParam("triplestoreID") String triplestoreID,
                               byte[] queryExecutionPlan,
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
                    byte[] triples,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

}
