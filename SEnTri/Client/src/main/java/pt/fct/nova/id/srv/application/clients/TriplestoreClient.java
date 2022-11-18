package pt.fct.nova.id.srv.application.clients;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreCreateException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreDeleteException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreSearchException;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreUploadException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.dtos.SearchResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import static pt.fct.nova.id.srv.presentation.Utils.buildResponse;


public class TriplestoreClient {

    private static final Gson gson = new Gson();
    private static final String SECURE_TRIPLESTORE_URI = System.getenv("SECURE_TRIPLESTORE_URI");
    private static final String CREATE_PATH = System.getenv("SECURE_CREATE_TRIPLESTORE_PATH");
    private static final String UPLOAD_PATH = System.getenv("SECURE_UPLOAD_TRIPLESTORE_PATH");
    private static final String DELETE_PATH = System.getenv("SECURE_DELETE_TRIPLESTORE_PATH");
    private static final String SEARCH_PATH = System.getenv("SECURE_SEARCH_TRIPLESTORE_PATH");

    private static final String ERROR_MESSAGE = "Error on %s request (%s): %s";
    private static final String CREATE = "create";
    private static final String UPLOAD = "upload";
    private static final String DELETE = "delete";
    private static final String SEARCH = "search";


    public static Response create(String storeID, Map<String, String> values) throws UnsupportedEncodingException, TriplestoreCreateException {
        try (Response response = buildResponse(sendRequest(storeID, new StringEntity(gson.toJson(values)), CREATE_PATH))) {
            if (response.getStatus() != Response.Status.OK.getStatusCode())
                throw new TriplestoreCreateException(String.format(ERROR_MESSAGE, CREATE, response.getStatus(), response.getEntity().toString()));
            else return response;
        }
    }

    public static Response upload(String storeID, Map<String, String> values) throws UnsupportedEncodingException, TriplestoreUploadException {
        try (Response response = buildResponse(sendRequest(storeID, new StringEntity(gson.toJson(values)), UPLOAD_PATH))) {
            if (response.getStatus() != Response.Status.OK.getStatusCode())
                throw new TriplestoreUploadException(String.format(ERROR_MESSAGE, UPLOAD, response.getStatus(), response.getEntity().toString()));
            else return response;
        }
    }

    public static List<String> search(String storeID, List<String> trapdoors) throws TriplestoreSearchException {
        try (CloseableHttpResponse response = sendRequest(storeID, new StringEntity(gson.toJson(trapdoors)), SEARCH_PATH)) {
            int status = Utils.buildResponse(response).getStatus();
            if (status != Response.Status.OK.getStatusCode())
                throw new TriplestoreSearchException(String.format(ERROR_MESSAGE, SEARCH, status, response.getEntity().toString()));
            return gson.fromJson(EntityUtils.toString(response.getEntity()), SearchResponse.class).searchResult();
        }  catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Response delete(String storeID, List<String> trapdoors) throws TriplestoreDeleteException {
        try (Response response = buildResponse(sendRequest(storeID, new StringEntity(gson.toJson(trapdoors)), DELETE_PATH))) {
            if (response.getStatus() != Response.Status.OK.getStatusCode())
                throw new TriplestoreDeleteException(String.format(ERROR_MESSAGE, DELETE, response.getStatus(), response.getEntity().toString()));
            else return response;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Response delete(String storeID) throws TriplestoreDeleteException {
        HttpDelete request = new HttpDelete(SECURE_TRIPLESTORE_URI + String.format(DELETE_PATH, storeID));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            try (Response response = buildResponse(client.execute(request))) {
                if (response.getStatus() != Response.Status.OK.getStatusCode())
                    throw new TriplestoreDeleteException(String.format(ERROR_MESSAGE, DELETE, response.getStatus(), response.getEntity().toString()));
                else return response;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static CloseableHttpResponse sendRequest(String storeID, HttpEntity body, String path) {
        HttpPost request = new HttpPost(SECURE_TRIPLESTORE_URI + String.format(path, storeID));
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
