package pt.fct.nova.id.srv.presentation.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_JSON_RESULTS;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.COOKIE_PARAM;

public interface EncryptedTriplestoreAPI {
    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response create(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm TriplestoreForm form);

    @DELETE
    @Path("/{triplestoreID}/{issuer}")
    @Produces(TEXT_PLAIN)
    Response delete(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    @PathParam("issuer") String issuer);

    @POST
    @Path("/upload")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response upload(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @DefaultValue("false") @QueryParam("schema") boolean schema,
                    @MultipartForm UploadForm form);

    @POST
    @Path("/schema")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_OCTET_STREAM)
    Response fetchSchema(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                         @DefaultValue("false") @QueryParam("inference") boolean inference,
                         @MultipartForm SchemaForm form);


    @POST
    @Path("query/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                               @MultipartForm QueryForm form);
}
