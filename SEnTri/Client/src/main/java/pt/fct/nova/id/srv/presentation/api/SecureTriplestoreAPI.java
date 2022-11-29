package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureCreateForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureQueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureUploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_JSON_RESULTS;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.COOKIE_PARAM;

public interface SecureTriplestoreAPI {
    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm SecureCreateForm form);

    @DELETE
    @Path("/{storeID}/{username}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("username") String username,
                    @PathParam("storeID") String storeID);

    @POST
    @Path("{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    @MultipartForm SecureUploadForm form);

    @POST
    @Path("query/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @MultipartForm SecureQueryForm form);
}
