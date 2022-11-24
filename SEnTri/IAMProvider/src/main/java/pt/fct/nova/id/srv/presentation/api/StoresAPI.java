package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.StoreForm;

import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.application.IAMStore.COOKIE_PARAM;

public interface StoresAPI {
    @POST
    @Path("")
    @Produces(TEXT_PLAIN)
    Response createStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @Form StoreForm form);

    @PUT
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response changeStoreOwner(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("username") String username,
                                     @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{storeID}")
    @Produces(TEXT_PLAIN)
    Response deleteStoreAccessPolicy(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                     @PathParam("storeID") String storeID,
                                     @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
