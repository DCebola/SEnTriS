package pt.fct.nova.id.srv.presentation.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SchemaForm;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.COOKIE_PARAM;

public interface TriplestoreAPI {

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response create(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm TriplestoreForm form);

    @POST
    @Path("/schema")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_OCTET_STREAM)
    Response fetchSchema(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                         @DefaultValue("false") @QueryParam("inference") boolean inference,
                         @MultipartForm SchemaForm form);

    @GET
    @Path("/{issuer}")
    @Produces(TEXT_PLAIN)
    Response listTriplestores(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("issuer") String issuer,
                              @DefaultValue("false") @QueryParam("write") boolean write,
                              @DefaultValue("false") @QueryParam("read") boolean read,
                              @DefaultValue("false") @QueryParam("owns") boolean owns);

    @POST
    @Path("/upload")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response upload(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @DefaultValue("false") @QueryParam("schema") boolean schema,
                    @MultipartForm UploadForm form);

    @DELETE
    @Path("/{triplestoreID}/{issuer}")
    @Produces(TEXT_PLAIN)
    Response delete(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    @PathParam("issuer") String issuer,
                    @DefaultValue("false") @QueryParam("schema") boolean schema);

    @POST
    @Path("/query")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                               @Form QueryForm form);

    @PUT
    @Path("/{triplestoreID}/{issuer}/access/{target}")
    @Produces(TEXT_PLAIN)
    Response grantAccess(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                         @PathParam("triplestoreID") String triplestoreID,
                         @PathParam("issuer") String issuer,
                         @PathParam("target") String target,
                         @DefaultValue("false") @QueryParam("write") boolean write);


    @DELETE
    @Path("/{triplestoreID}/{issuer}/access/{target}")
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("triplestoreID") String triplestoreID,
                          @PathParam("issuer") String issuer,
                          @PathParam("target") String target,
                          @DefaultValue("false") @QueryParam("write") boolean write);


    @POST
    @Path("/{triplestoreID}/{issuer}/access/requests")
    @Produces(TEXT_PLAIN)
    Response issueAccessRequest(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("triplestoreID") String triplestoreID,
                                @PathParam("issuer") String issuer,
                                @DefaultValue("false") @QueryParam("write") boolean write);

    @PUT
    @Path("/{triplestoreID}/{issuer}/owner/{target}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response updateTriplestoreOwner(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("triplestoreID") String triplestoreID,
                                    @PathParam("issuer") String issuer,
                                    @PathParam("target") String target);

    @GET
    @Path("/{triplestoreID}/{issuer}/access/users")
    @Produces(TEXT_PLAIN)
    Response listUsersWithAccess(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("triplestoreID") String triplestoreID,
                                 @PathParam("issuer") String issuer,
                                 @DefaultValue("false") @QueryParam("write") boolean write);

    @GET
    @Path("/{triplestoreID}/{issuer}/access/requests")
    @Produces(TEXT_PLAIN)
    Response listPendingAccessRequests(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                       @PathParam("triplestoreID") String triplestoreID,
                                       @PathParam("issuer") String issuer);

    @PUT
    @Path("/{triplestoreID}/{issuer}/access/requests/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processPendingAccessRequest(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                         @PathParam("triplestoreID") String triplestoreID,
                                         @PathParam("issuer") String issuer,
                                         @PathParam("requestID") String requestID,
                                         @DefaultValue("false") @QueryParam("accept") boolean decision);

}
