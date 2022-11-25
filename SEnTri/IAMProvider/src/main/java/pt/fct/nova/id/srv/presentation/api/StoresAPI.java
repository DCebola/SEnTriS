package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.StoreForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface StoresAPI {
    @POST
    @Path("")
    @Produces(TEXT_PLAIN)
    Response createStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @Form StoreForm form);

    @GET
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response listStores(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username);

    @PUT
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response changeStoreOwner(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("username") String username,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{storeID}")
    @Produces(TEXT_PLAIN)
    Response deleteStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("storeID") String storeID,
                                     @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{storeID}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response grantAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                         @PathParam("storeID") String storeID,
                         @PathParam("username") String username,
                         @DefaultValue("false") @QueryParam("write") boolean write,
                         @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{storeID}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("storeID") String storeID,
                          @PathParam("username") String username,
                          @DefaultValue("false") @QueryParam("write") boolean write,
                          @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{storeID}/access/requests")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("storeID") String storeID,
                                @Form AccessForm accessForm);

    @GET
    @Path("/{storeID}/access/requests")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                      @PathParam("storeID") String storeID,
                                      @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{storeID}/access/requests/{requestID}")
    @Produces(TEXT_PLAIN)
    Response processAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("storeID") String storeID,
                                  @PathParam("requestID") String requestID,
                                  @DefaultValue("false") @QueryParam("accept") boolean decision,
                                  @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{storeID}/access/tokens/{username}")
    @Produces(TEXT_PLAIN)
    Response createAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("storeID") String storeID,
                               @PathParam("username") String username);

    @DELETE
    @Path("/{storeID}/tokens")
    @Produces(TEXT_PLAIN)
    Response deleteAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("storeID") String storeID,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{storeID}/access/tokens/read")
    @Produces(TEXT_PLAIN)
    Response checkReadAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                             @PathParam("storeID") String storeID,
                             @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{storeID}/access/tokens/write")
    @Produces(TEXT_PLAIN)
    Response checkWriteAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{storeID}/access/tokens/owner")
    @Produces(TEXT_PLAIN)
    Response checkOwnerAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/{storeID}/locks")
    @Produces(TEXT_PLAIN)
    Response acquireStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{storeID}/locks")
    @Produces(TEXT_PLAIN)
    Response releaseStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
