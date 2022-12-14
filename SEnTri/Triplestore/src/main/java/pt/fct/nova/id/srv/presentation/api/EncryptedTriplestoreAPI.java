package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.*;
import static pt.fct.nova.id.srv.application.clients.HTTPUtils.COOKIE_PARAM;

public interface EncryptedTriplestoreAPI {

    @POST
    @Path("{triplestoreID}")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    Map<String, String> encryptedNodes,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/bind")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response prepareSPARQLQueryBindings(@CookieParam(COOKIE_PARAM) Cookie cookie,
                                        @PathParam("triplestoreID") String triplestoreID,
                                        List<String> trapdoors,
                                        @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("{triplestoreID}/search")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response search(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    List<String> trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @DELETE
    @Path("/{triplestoreID}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);

    @POST
    @Path("/{triplestoreID}/delete")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("triplestoreID") String triplestoreID,
                    List<String> trapdoors,
                    @HeaderParam(AUTHORIZATION) List<String> authorizationHeaders);
}
