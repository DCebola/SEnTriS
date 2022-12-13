package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.COOKIE_PARAM;

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
    @Path("/{username}/upgrade")
    @Produces(TEXT_PLAIN)
    Response issueUpgradeRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("username") String username);

    @POST
    @Path("/{username}/downgrade")
    @Produces(TEXT_PLAIN)
    Response issueDowngradeRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username);

    @GET
    @Path("{username}/requests")
    @Produces(TEXT_PLAIN)
    Response listPendingRequests(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("username") String username);

    @PUT
    @Path("{username}/requests/{requestID}")
    @Produces(TEXT_PLAIN)
    Response processPendingRequest(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @PathParam("requestID") String requestID,
                                   @Form RequestDecisionForm decisionForm);
}
