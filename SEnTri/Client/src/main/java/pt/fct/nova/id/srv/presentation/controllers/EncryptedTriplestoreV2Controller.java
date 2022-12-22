package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.clients.*;
import pt.fct.nova.id.srv.presentation.api.EncryptedTriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.QueryForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.*;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoreController.INTERNAL_ERROR;

@Path("triplestores/v2/encrypted")
public class EncryptedTriplestoreV2Controller extends EncryptedTriplestoreController implements EncryptedTriplestoreAPI{

    @Override
    public Response create(Cookie cookie, UploadForm form) {
        return Response.ok("Not Implemented").status(NOT_IMPLEMENTED).build();
    }

    @Override
    public Response delete(Cookie cookie, String triplestoreID, String issuer) {
        try (CloseableHttpClient httpClient = HTTPClient.buildClient()) {
            HTTPResponse response = EncryptedTriplestoreController.deleteEncryptedTriplestore(httpClient, cookie, triplestoreID, issuer);
            if (response.getStatus() != OK)
                return response.build();
            return Response.ok(SUCCESSFUL_DELETION).build();
        } catch (Exception e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, UploadForm form) {
        return Response.ok("Not Implemented").status(NOT_IMPLEMENTED).build();
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, QueryForm form) {
        return Response.ok("Not Implemented").status(NOT_IMPLEMENTED).build();
    }
}
