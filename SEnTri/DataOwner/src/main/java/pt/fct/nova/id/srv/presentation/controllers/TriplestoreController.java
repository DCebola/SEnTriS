package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.TriplestoreAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;
import pt.fct.nova.id.srv.presentation.clients.HTTPSClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

@Path("triplestore")
public class TriplestoreController implements TriplestoreAPI {

    private static final String HTTP_CLIENT_ERROR = "Internal server error: DataOwner client.";
    private static final String TRIPLESTORE_URI = System.getenv("TRIPLESTORE_URI");

    private static final String HOST = System.getenv("HOST");
    private static final String CREATE_TRIPLESTORE_PATH = System.getenv("CREATE_TRIPLESTORE_PATH");
    private static final String UPLOAD_TRIPLESTORE_PATH = System.getenv("UPLOAD_TRIPLESTORE_PATH");
    private static final String QUERY_TRIPLESTORE_PATH = System.getenv("QUERY_TRIPLESTORE_PATH");

    @Override
    public Response create(String storeID, UploadForm form) {
        return sendRequest(storeID, Utils.uploadFormToHttpEntity(form), CREATE_TRIPLESTORE_PATH);
    }

    @Override
    public Response upload(String storeID, UploadForm form) {
        return sendRequest(storeID, Utils.uploadFormToHttpEntity(form), UPLOAD_TRIPLESTORE_PATH);
    }

    @Override
    public Response answerSPARQLQuery(String storeID, String query) {
        return sendRequest(storeID, new StringEntity(query, StandardCharsets.UTF_8), QUERY_TRIPLESTORE_PATH);
    }

    private Response sendRequest(String storeID, HttpEntity body, String path) {
        HttpPost request = new HttpPost(TRIPLESTORE_URI + String.format(path, storeID));
        request.setEntity(body);
        System.out.println(request);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return Utils.buildResponse(client.execute(request));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return Response.ok(HTTP_CLIENT_ERROR).status(INTERNAL_SERVER_ERROR).build();
    }
}
