package pt.fct.nova.id.srv.presentation.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import pt.fct.nova.id.srv.presentation.api.dtos.SecureSPARQLQueryForm;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.*;

public interface ProxyAPI {

    @POST
    @Path("/")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(APPLICATION_JSON)
    Response answerSPARQLQuery(@MultipartForm SecureSPARQLQueryForm form);

    @POST
    @Path("/bindings")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(TEXT_PLAIN)
    Response saveBinding(List<String> encryptedNodes);
}
