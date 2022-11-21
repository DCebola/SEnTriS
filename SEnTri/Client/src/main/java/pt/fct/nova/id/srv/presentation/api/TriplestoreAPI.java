package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static pt.fct.nova.id.srv.application.clients.HttpUtils.COOKIE_PARAM;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_QUERY;

public interface TriplestoreAPI {

    @POST
    @Path("create/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    @MultipartForm UploadForm form);

    @POST
    @Path("upload/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    @MultipartForm UploadForm form);

    @POST
    @Path("/query/{storeID}")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("storeID") String storeID,
                               String query);

}
