package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.*;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface IdentityAndAccessManagementAPI {

    @POST
    @Path("auth")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response auth(@Form AuthForm credentialsForm);

    @POST
    @Path("users")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response registerUser(@Form AuthForm credentialsForm);

    @DELETE
    @Path("users/{username}")
    @Produces(TEXT_PLAIN)
    Response deleteUser(@CookieParam(COOKIE_PARAM) Cookie cookie,
                        @PathParam("username") String username);

    @DELETE
    @Path("users/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("username") String username,
                          @Form AccessForm accessForm);

    @POST
    @Path("users/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @Form AccessForm accessForm);

    @POST
    @Path("users/{username}/role")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @Form RoleForm roleForm);

    @GET
    @Path("pending/{username}/access")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                      @PathParam("username") String username);

    @GET
    @Path("pending/{username}/role")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("username") String username);

    @GET
    @Path("pending/{username}/access/{requestID}")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @PathParam("requestID") String requestID);

    @GET
    @Path("pending/{username}/role/{requestID}")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @PathParam("requestID") String requestID);

    @POST
    @Path("pending/{storeID}/access/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("storeID") String storeID,
                                  @PathParam("requestID") String requestID,
                                  @Form RequestDecisionForm requestDecisionForm);

    @POST
    @Path("pending/{username}/role/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("username") String username,
                                @PathParam("requestID") String requestID,
                                @Form RequestDecisionForm requestDecisionForm);

    @POST
    @Path("/{username}/stores")
    @Produces(TEXT_PLAIN)
    Response createStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @Form StoreForm form);

    @DELETE
    @Path("/{username}/stores/{storeID}")
    @Produces(TEXT_PLAIN)
    Response deleteStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @PathParam("storeID") String storeID);

    @GET
    @Path("stores/{storeID}/access/read")
    @Produces(TEXT_PLAIN)
    Response getReadAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                           @PathParam("storeID") String storeID,
                           @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("stores/{storeID}/access/write")
    @Produces(TEXT_PLAIN)
    Response getWriteAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                            @PathParam("storeID") String storeID,
                            @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("stores/{storeID}/access/owner")
    @Produces(TEXT_PLAIN)
    Response getOwnerAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                            @PathParam("storeID") String storeID,
                            @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("access-tokens/{username}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response createAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("username") String username,
                               @Form StoreForm form);

    @DELETE
    @Path("access-tokens/{username}")
    @Produces(TEXT_PLAIN)
    Response deleteAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("username") String username,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

}
