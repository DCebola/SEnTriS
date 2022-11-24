package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.*;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface UsersAPI {

    @POST
    @Path("/auth")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response auth(@Form AuthForm credentialsForm);

    @POST
    @Path("")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response registerUser(@Form AuthForm credentialsForm);

    @DELETE
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response deleteUser(@CookieParam(COOKIE_PARAM) Cookie cookie,
                        @PathParam("username") String username);

    @DELETE
    @Path("/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("username") String username,
                          @Form AccessForm accessForm);

    @POST
    @Path("/{username}/access")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @Form AccessForm accessForm);

    @POST
    @Path("/{username}/role")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @Form RoleForm roleForm);

    @GET
    @Path("/{username}/requests/access")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                      @PathParam("username") String username);

    @GET
    @Path("/{username}/requests/role")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("username") String username);

    @GET
    @Path("/{username}/requests/access/{requestID}")
    @Produces(APPLICATION_JSON)
    Response getPendingAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @PathParam("requestID") String requestID);

    @GET
    @Path("/{username}/requests/role/{requestID}")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @PathParam("requestID") String requestID);

    @POST
    @Path("/{username}/requests/access/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processAccessRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                  @PathParam("username") String username,
                                  @PathParam("requestID") String requestID,
                                  @Form RequestDecisionForm requestDecisionForm);

    @POST
    @Path("/{username}/requests/role/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("username") String username,
                                @PathParam("requestID") String requestID,
                                @Form RequestDecisionForm requestDecisionForm);
}
