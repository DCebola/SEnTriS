package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import pt.fct.nova.id.srv.presentation.api.dtos.SecretsForm;

import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.presentation.Utils.COOKIE_PARAM;

public interface SecretsAPI {

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response createSecrets(@CookieParam(COOKIE_PARAM) Cookie cookie,
                           @Form SecretsForm secrets);
    @GET
    @Path("{username}/{storeID}")
    @Produces(APPLICATION_JSON)
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
