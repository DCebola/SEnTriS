package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.EncryptedCreateForm;
import pt.fct.nova.id.srv.presentation.api.dtos.EncryptedQueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.EncryptedUploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_JSON_RESULTS;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.COOKIE_PARAM;

public interface EncryptedTriplestoreAPI {
    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm EncryptedCreateForm form);

    @DELETE
    @Path("/{triplestoreID}/{issuer}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    @PathParam("issuer") String issuer);

    @POST
    @Path("/upload")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm EncryptedUploadForm form);

    @POST
    @Path("query/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @MultipartForm EncryptedQueryForm form);
}
