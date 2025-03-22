package pt.fct.nova.id.srv.presentation.apis;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.dtos.TriplestoreForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.storage.redis.IAMStorage.COOKIE_PARAM;

public interface TriplestoresAPI {
    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response createTriplestoreAccessList(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                           @MultipartForm TriplestoreForm form);

    @GET
    @Path("/{issuer}")
    @Produces(TEXT_PLAIN)
    Response listTriplestores(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("issuer") String issuer,
                              @DefaultValue("false") @QueryParam("write") boolean write,
                              @DefaultValue("true") @QueryParam("read") boolean read,
                              @DefaultValue("false") @QueryParam("owns") boolean owns);

    @GET
    @Path("/{triplestoreID}/access/users")
    @Produces(TEXT_PLAIN)
    Response listUsersWithAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("triplestoreID") String triplestoreID,
                                 @DefaultValue("false") @QueryParam("write") boolean write,
                                 @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @PUT
    @Path("/{triplestoreID}/owner/{target}")
    @Produces(TEXT_PLAIN)
    Response changeTriplestoreOwner(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("triplestoreID") String triplestoreID,
                                    @PathParam("target") String target,
                                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response deleteTriplestoreAccessList(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                           @PathParam("triplestoreID") String triplestoreID,
                                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @PUT
    @Path("/{triplestoreID}/access/{target}")
    @Produces(TEXT_PLAIN)
    Response grantAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                         @PathParam("triplestoreID") String triplestoreID,
                         @PathParam("target") String target,
                         @DefaultValue("false") @QueryParam("write") boolean write,
                         @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}/access/{target}")
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("triplestoreID") String triplestoreID,
                          @PathParam("target") String target,
                          @DefaultValue("false") @QueryParam("write") boolean write,
                          @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/access/requests/{target}")
    @Produces(TEXT_PLAIN)
    Response issueAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("triplestoreID") String triplestoreID,
                                @PathParam("target") String target,
                                @DefaultValue("false") @QueryParam("write") boolean write);

    @GET
    @Path("/{triplestoreID}/access/requests")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                      @PathParam("triplestoreID") String triplestoreID,
                                      @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @PUT
    @Path("/{triplestoreID}/access/requests/{requestID}")
    @Produces(TEXT_PLAIN)
    Response processAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("triplestoreID") String triplestoreID,
                                  @PathParam("requestID") String requestID,
                                  @DefaultValue("false") @QueryParam("accept") boolean decision,
                                  @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/access/tokens/{target}")
    @Produces(TEXT_PLAIN)
    Response createAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("triplestoreID") String triplestoreID,
                               @PathParam("target") String target);


    @DELETE
    @Path("/{triplestoreID}/access/tokens")
    @Produces(TEXT_PLAIN)
    Response deleteAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("triplestoreID") String triplestoreID,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{triplestoreID}/access/tokens/read")
    @Produces(TEXT_PLAIN)
    Response checkReadAccess(@PathParam("triplestoreID") String triplestoreID,
                             @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{triplestoreID}/access/tokens/write")
    @Produces(TEXT_PLAIN)
    Response checkWriteAccess(@PathParam("triplestoreID") String triplestoreID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{triplestoreID}/access/tokens/owner")
    @Produces(TEXT_PLAIN)
    Response checkOwnerAccess(@PathParam("triplestoreID") String triplestoreID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/access/locks")
    @Produces(TEXT_PLAIN)
    Response acquireTriplestoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("triplestoreID") String triplestoreID,
                                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}/access/locks")
    @Produces(TEXT_PLAIN)
    Response releaseTriplestoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("triplestoreID") String triplestoreID,
                                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
