package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

public interface SecretsAPI {
    String COOKIE_PARAM = "session";

    @POST
    @Path("{username}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response createSecrets(@CookieParam(COOKIE_PARAM) Cookie cookie,
                           @PathParam("username") String username,
                           List<String> secrets);
    @GET
    @Path("{username}/{storeID}")
    @Produces(TEXT_PLAIN)
    Response getSecrets(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("username") String username,
                              @PathParam("storeID") String storeID);

    @DELETE
    @Path("{username}/{storeID}")
    @Produces(TEXT_PLAIN)
    Response deleteSecrets(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("username") String username,
                              @PathParam("storeID") String storeID);
}
