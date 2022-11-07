package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.api.dtos.SearchBody;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;

public interface EncryptedTriplestoreAPI {

    @POST
    @Path("create/{storeID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response create(
            @PathParam("storeID") String storeID,
            Map<String, String> encryptedNodes);

    @POST
    @Path("upload/{storeID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response upload(
            @PathParam("storeID") String storeID,
            Map<String, String> encryptedNodes);

    @POST
    @Path("/query/{storeID}")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@PathParam("storeID") String storeID, QueryExecutionPlan queryExecutionPlan);

    @DELETE
    @Path("/delete/{storeID}")
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("storeID") String storeID);
}
