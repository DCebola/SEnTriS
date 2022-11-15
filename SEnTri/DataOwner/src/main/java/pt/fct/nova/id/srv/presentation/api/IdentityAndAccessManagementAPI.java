package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.*;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.clients.iam.IAMStore.COOKIE_PARAM;

public interface IdentityAndAccessManagementAPI {

    @POST
    @Path("auth")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response auth(@Form AuthForm credentialsForm);

    @POST
    @Path("")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response registerUser(@Form AuthForm credentialsForm);

    @DELETE
    @Path("{username}")
    @Produces(TEXT_PLAIN)
    Response deleteUser(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username);

    @DELETE
    @Path("{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, @Form AccessPolicyForm accessPolicyForm);

    @POST
    @Path("{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, @Form AccessPolicyForm accessPolicyForm);

    @POST
    @Path("{username}/role")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, @Form RoleForm roleForm);

    @GET
    @Path("{username}/access-requests")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username);

    @DELETE
    @Path("access-requests/{requestID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response processAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("requestID") String requestID,
                                  @Form RequestDecisionForm requestDecisionForm);

    @GET
    @Path("{username}/role-requests")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username);

    @DELETE
    @Path("role-requests/{requestID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response processRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("requestID") String requestID,
                                @Form RequestDecisionForm requestDecisionForm);


    @GET
    @Path("{username}/store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response getStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("username") String username,
                                  @PathParam("storeID") String storeID);

    @POST
    @Path("{username}/store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response createStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @PathParam("storeID") String storeID,
                                     @Form AccessPolicyForm accessPolicy);

    @DELETE
    @Path("{username}/store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response deleteStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @PathParam("storeID") String storeID);

}
