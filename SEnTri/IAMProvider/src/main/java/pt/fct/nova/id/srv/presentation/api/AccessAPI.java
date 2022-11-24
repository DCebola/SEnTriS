package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.StoreForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface AccessAPI {

    @POST
    @Path("/tokens")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response createAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @Form StoreForm form);

    @DELETE
    @Path("/tokens")
    @Produces(TEXT_PLAIN)
    Response deleteAccessToken(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/locks")
    @Produces(TEXT_PLAIN)
    Response acquireStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/locks")
    @Produces(TEXT_PLAIN)
    Response releaseStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/read/{storeID}")
    @Produces(TEXT_PLAIN)
    Response checkReadAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                             @PathParam("storeID") String storeID,
                             @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/write/{storeID}")
    @Produces(TEXT_PLAIN)
    Response checkWriteAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @GET
    @Path("/owner/{storeID}")
    @Produces(TEXT_PLAIN)
    Response checkOwnerAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("storeID") String storeID,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
