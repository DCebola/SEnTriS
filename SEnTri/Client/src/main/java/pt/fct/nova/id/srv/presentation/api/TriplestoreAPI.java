package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_QUERY;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.COOKIE_PARAM;

public interface TriplestoreAPI {

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response create(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm UploadForm form);

    @GET
    @Path("/{issuer}")
    @Produces(TEXT_PLAIN)
    Response listTriplestores(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("issuer") String issuer,
                              @DefaultValue("false") @QueryParam("write") boolean write,
                              @DefaultValue("false") @QueryParam("read") boolean read,
                              @DefaultValue("false") @QueryParam("owns") boolean owns);

    @POST
    @Path("/{triplestoreID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    @MultipartForm UploadForm form);

    @DELETE
    @Path("/{triplestoreID}/{username}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("username") String username,
                    @PathParam("triplestoreID") String triplestoreID);

    @POST
    @Path("/query")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @Form QueryForm form);

    @PUT
    @Path("/{triplestoreID}/{issuer}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response grantAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                         @PathParam("triplestoreID") String triplestoreID,
                         @PathParam("issuer") String issuer,
                         @PathParam("username") String username,
                         @DefaultValue("false") @QueryParam("write") boolean write);


    @DELETE
    @Path("/{triplestoreID}/{issuer}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("triplestoreID") String triplestoreID,
                          @PathParam("issuer") String issuer,
                          @PathParam("username") String username,
                          @DefaultValue("false") @QueryParam("write") boolean write);


    @PUT
    @Path("/{triplestoreID}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("triplestoreID") String triplestoreID,
                                @Form AccessForm form);

    @PUT
    @Path("/{triplestoreID}/{issuer}/owner/{username}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response updateTriplestoreOwner(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("triplestoreID") String triplestoreID,
                                    @PathParam("issuer") String issuer,
                                    @PathParam("username") String username);

    @GET
    @Path("/{triplestoreID}/{issuer}/access/users")
    @Produces(TEXT_PLAIN)
    Response listUsersWithAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("triplestoreID") String triplestoreID,
                                 @PathParam("issuer") String issuer,
                                 @DefaultValue("false") @QueryParam("write") boolean write);

    @GET
    @Path("/{triplestoreID}/{issuer}/access/requests")
    @Produces(TEXT_PLAIN)
    Response listPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                       @PathParam("triplestoreID") String triplestoreID,
                                       @PathParam("issuer") String issuer);

    @PUT
    @Path("/{triplestoreID}/{issuer}/access/requests/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processPendingAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                         @PathParam("triplestoreID") String triplestoreID,
                                         @PathParam("issuer") String issuer,
                                         @PathParam("requestID") String requestID,
                                         @DefaultValue("false") @QueryParam("accept") boolean decision);

}
