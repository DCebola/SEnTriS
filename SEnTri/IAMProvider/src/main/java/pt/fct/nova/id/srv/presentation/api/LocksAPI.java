package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface LocksAPI {

    @GET
    @Path("")
    @Produces(TEXT_PLAIN)
    Response acquireStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("")
    @Produces(TEXT_PLAIN)
    Response releaseStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
