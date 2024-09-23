package pt.fct.nova.id.srv.presentation.api;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.COOKIE_PARAM;

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
    Response deleteUser(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                        @PathParam("username") String username);

    @POST
    @Path("/{username}/upgrade")
    @Produces(TEXT_PLAIN)
    Response issueUpgradeRequest(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("username") String username);

    @POST
    @Path("/{username}/downgrade")
    @Produces(TEXT_PLAIN)
    Response issueDowngradeRequest(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username);

    @GET
    @Path("{username}/requests")
    @Produces(TEXT_PLAIN)
    Response listPendingRequests(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                 @PathParam("username") String username);

    @PUT
    @Path("{username}/requests/{requestID}")
    @Produces(TEXT_PLAIN)
    Response processPendingRequest(@NotNull @CookieParam(COOKIE_PARAM) Cookie cookie,
                                   @PathParam("username") String username,
                                   @PathParam("requestID") String requestID,
                                   @MultipartForm RequestDecisionForm decisionForm);
}
