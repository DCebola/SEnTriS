package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.IAMRequestForm;

import static jakarta.ws.rs.core.MediaType.*;

public interface IdentityAndAccessManagementAPI {
    String COOKIE_PARAM = "session:";

    @POST
    @Path("register")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response register(@Form AuthForm credentials);

    @DELETE
    @Path("delete/{username}")
    @Produces(TEXT_PLAIN)
    Response delete(@PathParam("username") String username);

    @POST
    @Path("auth")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response auth(@Form AuthForm credentials);

    @GET
    @Path("requests")
    @Produces(APPLICATION_JSON)
    Response listPendingIAMRequests(@CookieParam(COOKIE_PARAM) Cookie cookie, @Form IAMRequestForm request);

    @DELETE
    @Path("requests")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response processIAMRequest(@CookieParam(COOKIE_PARAM) Cookie cookie, @Form IAMRequestForm request);

    @POST
    @Path("requests")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response issueIAMRequest(@CookieParam(COOKIE_PARAM) Cookie cookie, @Form IAMRequestForm request);

}
