package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface LocksAPI {

    @GET
    @Path("{username}/stores/{storeID}")
    @Produces(TEXT_PLAIN)
    Response acquireStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("username") String username,
                              @PathParam("storeID") String storeID);

    @DELETE
    @Path("{lockID}/{username}/store/{storeID}")
    @Produces(TEXT_PLAIN)
    Response releaseStoreLock(@CookieParam(COOKIE_PARAM) Cookie cookie,
                              @PathParam("lockID") String lockID,
                              @PathParam("username") String username,
                              @PathParam("storeID") String storeID);
}
