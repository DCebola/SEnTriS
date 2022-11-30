package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.*;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.IAMStorage.COOKIE_PARAM;

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

    @POST
    @Path("/{username}/role/requests")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueGrantRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @Form RoleForm roleForm);

    @GET
    @Path("/{username}/role/requests")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("username") String username);

    @PUT
    @Path("/{username}/role/requests/{requestID}")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("username") String username,
                                @PathParam("requestID") String requestID,
                                @Form RequestDecisionForm requestDecisionForm);
}
