package pt.fct.nova.id.srv.presentation.apis;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.dtos.*;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.IAMStorage.COOKIE_PARAM;

public interface UsersAPI {

    @POST
    @Path("/auth")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response auth(@MultipartForm AuthForm credentialsForm);

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response registerUser(@MultipartForm AuthForm credentialsForm);

    @DELETE
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response deleteUser(@CookieParam(COOKIE_PARAM) Cookie cookie,
                        @PathParam("username") String username);

    @POST
    @Path("/{username}/role/requests")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response issueGrantRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @MultipartForm RoleForm roleForm);

    @GET
    @Path("/{username}/role/requests")
    @Produces(APPLICATION_JSON)
    Response getPendingRoleRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                    @PathParam("username") String username);

    @PUT
    @Path("/{username}/role/requests/{requestID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response processRoleRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                @PathParam("username") String username,
                                @PathParam("requestID") String requestID,
                                @MultipartForm RequestDecisionForm requestDecisionForm);

    @GET
    @Path("/tokens/active")
    @Produces(TEXT_PLAIN)
    Response checkIfActive(@HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
