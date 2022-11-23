package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static jakarta.ws.rs.core.Response.Status.OK;


@Path("triplestore")
public class TriplestoreController implements TriplestoreAPI {
    private static final String INTERNAL_ERROR = "Internal error.";

    private static final String SUCCESSFUL_UPLOAD = "Successful upload.";
    private static final String TRIPLESTORE_URI = System.getenv("TRIPLESTORE_URI");
    private static final String CREATE_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("CREATE_TRIPLESTORE_PATH"));
    private static final String UPLOAD_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("UPLOAD_TRIPLESTORE_PATH"));
    private static final String QUERY_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("QUERY_TRIPLESTORE_PATH"));

    @Override
    public Response create(Cookie cookie, String storeID, UploadForm form) {
        /* TODO: Create access policy
        String storeID = form.getStoreID();
        String issuer = form.getIssuer();
        try (CloseableHttpResponse response = IAMClient.createStore(cookie, issuer, storeID)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HttpUtils.buildResponse(response);
        }
         */
        if (form.getSyntax() != null && form.getContents() != null) {
            try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                    String.format(CREATE_TRIPLESTORE_PATH, storeID), ClientUtils.uploadFormToHttpEntity(form))) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode()) {
                    //DELETE Store
                }
                return HttpUtils.buildResponse(response);
            } catch (IOException e) {
                return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        return Response.ok(SUCCESSFUL_UPLOAD).build();
    }

    @Override
    public Response upload(Cookie cookie, String storeID, UploadForm form) {
        try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                String.format(UPLOAD_TRIPLESTORE_PATH, storeID), ClientUtils.uploadFormToHttpEntity(form))) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, String storeID, String query) {
        try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                String.format(QUERY_TRIPLESTORE_PATH, storeID), new StringEntity(query, StandardCharsets.UTF_8))) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
