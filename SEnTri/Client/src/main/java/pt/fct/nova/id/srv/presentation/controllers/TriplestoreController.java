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

import static jakarta.ws.rs.core.Response.Status.*;


@Path("triplestore")
public class TriplestoreController implements TriplestoreAPI {
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String MALFORMED_FORM = "Malformed form.";
    private static final String TRIPLESTORE_URI = System.getenv("TRIPLESTORE_URI");
    private static final String CREATE_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("CREATE_TRIPLESTORE_PATH"));
    private static final String UPLOAD_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("UPLOAD_TRIPLESTORE_PATH"));
    private static final String QUERY_TRIPLESTORE_PATH = TRIPLESTORE_URI.concat(System.getenv("QUERY_TRIPLESTORE_PATH"));


    @Override
    public Response create(Cookie cookie, UploadForm form) {
        try {
            String storeID = form.getStoreID();
            String issuer = form.getIssuer();
            try (CloseableHttpResponse response = IAMClient.createStore(cookie, storeID, issuer)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
            }

            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, issuer, storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            return upload(cookie, storeID, form, accessToken, CREATE_TRIPLESTORE_PATH);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response upload(Cookie cookie, String storeID, UploadForm form) {
        try {
            String accessToken;
            try (CloseableHttpResponse response = IAMClient.getAccessToken(cookie, form.getIssuer(), storeID)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                accessToken = response.getEntity().toString();
            }
            return upload(cookie, storeID, form, accessToken, UPLOAD_TRIPLESTORE_PATH);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response upload(Cookie cookie, String storeID, UploadForm form, String accessToken, String uploadTriplestorePath) throws IOException {
        try (CloseableHttpResponse response = IAMClient.acquireStoreLock(cookie, storeID, accessToken)) {
            if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                return HttpUtils.buildResponse(response);
        }
        if (form.getSyntax() != null && form.getContents() != null) {
            try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                    String.format(uploadTriplestorePath, storeID),
                    ClientUtils.uploadFormToHttpEntity(form),
                    accessToken)) {
                IAMClient.releaseStoreLock(cookie, storeID, accessToken);
                return HttpUtils.buildResponse(response);
            }
        }
        return Response.ok(MALFORMED_FORM).status(BAD_REQUEST).build();
    }

    @Override
    public Response answerSPARQLQuery(Cookie cookie, String storeID, String query) {
        //TODO: Migrate query planner to client. Send query execution.
        try (CloseableHttpResponse response = HttpUtils.sendPOSTRequest(cookie,
                String.format(QUERY_TRIPLESTORE_PATH, storeID),
                new StringEntity(query, StandardCharsets.UTF_8)
        )) {
            return HttpUtils.buildResponse(response);
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

}
