package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.clients.HttpUtils.COOKIE_PARAM;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;


public interface TriplestoreAPI {

    @POST
    @Path("{storeID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    List<Triple> triples,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/query/{storeID}")
    @Consumes(APPLICATION_JSON)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("storeID") String storeID,
                               QueryExecutionPlan queryExecutionPlan,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{storeID}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{storeID}/delete")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    List<Triple> triples,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

}
