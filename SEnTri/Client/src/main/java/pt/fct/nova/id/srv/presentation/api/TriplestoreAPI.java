package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.*;
import static pt.fct.nova.id.srv.presentation.api.RDFMediaType.SPARQL_QUERY;
import static pt.fct.nova.id.srv.presentation.controllers.ClientUtils.COOKIE_PARAM;

public interface TriplestoreAPI {

    @POST
    @Path("")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response create(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @MultipartForm UploadForm form);

    @GET
    @Path("/{username}")
    @Produces(TEXT_PLAIN)
    Response listStores(@CookieParam(COOKIE_PARAM) Cookie cookie,
                        @PathParam("username") String username,
                        @DefaultValue("false") @QueryParam("write") boolean write,
                        @DefaultValue("false") @QueryParam("read") boolean read,
                        @DefaultValue("false") @QueryParam("owns") boolean owns);

    @POST
    @Path("/{storeID}")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    Response upload(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("storeID") String storeID,
                    @MultipartForm UploadForm form);

    @DELETE
    @Path("/{storeID}/{username}")
    @Produces(TEXT_PLAIN)
    Response delete(@CookieParam(COOKIE_PARAM) Cookie cookie,
                    @PathParam("username") String username,
                    @PathParam("storeID") String storeID);

    @POST
    @Path("/query/{storeID}")
    @Consumes(SPARQL_QUERY)
    @Produces(SPARQL_JSON_RESULTS)
    Response answerSPARQLQuery(@CookieParam(COOKIE_PARAM) Cookie cookie,
                               @PathParam("storeID") String storeID,
                               String query);

    @POST
    @Path("/{storeID}/access/requests")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response requestAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                           @PathParam("storeID") String storeID,
                           @Form AccessForm form);

    @POST
    @Path("/{storeID}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response grantAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                         @PathParam("storeID") String storeID,
                         @PathParam("username") String username,
                         @DefaultValue("false") @QueryParam("write") boolean write);

    @DELETE
    @Path("/{storeID}/access/{username}")
    @Produces(TEXT_PLAIN)
    Response revokeAccess(@CookieParam(COOKIE_PARAM) Cookie cookie,
                          @PathParam("storeID") String storeID,
                          @PathParam("username") String username,
                          @DefaultValue("false") @QueryParam("write") boolean write);
}
