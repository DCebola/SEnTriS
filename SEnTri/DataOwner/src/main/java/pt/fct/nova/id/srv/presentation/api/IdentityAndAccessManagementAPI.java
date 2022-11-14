package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UserDTO;

import static jakarta.ws.rs.core.MediaType.*;

public interface IdentityAndAccessManagementAPI {
    String COOKIE_PARAM = "session:";

    @POST
    @Path("auth")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response auth(@Form AuthForm credentials);

    @POST
    @Path("users")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response addUser(@Form AuthForm credentials);

    @PUT
    @Path("users/{username}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response updateUser(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, UserDTO user);

    @DELETE
    @Path("users/{username}")
    @Produces(TEXT_PLAIN)
    Response deleteUSer(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username);

    @POST
    @Path("users/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, @Form AccessPolicyForm accessPolicy);

    @DELETE
    @Path("users/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("username") String username, @Form AccessPolicyForm accessPolicy);

    @GET
    @Path("pending-role-requests")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie);

    @GET
    @Path("store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response getStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("storeID") String storeID);

    @POST
    @Path("store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response createStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("storeID") String storeID, @Form AccessPolicyForm accessPolicy);

    @PUT
    @Path("store-access-policies/{storeID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    Response grantAccess(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("storeID") String storeID, @Form AccessPolicyForm accessPolicy);

    @DELETE
    @Path("store-access-policies/{storeID}")
    @Produces(APPLICATION_JSON)
    Response deleteStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie, @PathParam("storeID") String storeID);

}
