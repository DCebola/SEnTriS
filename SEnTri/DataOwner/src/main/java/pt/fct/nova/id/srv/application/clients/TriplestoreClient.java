package pt.fct.nova.id.srv.application.clients;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.clients.exception.TriplestoreClientException;
import pt.fct.nova.id.srv.presentation.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


public class TriplestoreClient {

    private static final Gson gson = new Gson();
    private static final String SECURE_TRIPLESTORE_URI = System.getenv("SECURE_TRIPLESTORE_URI");
    private static final String CREATE_PATH = System.getenv("SECURE_CREATE_TRIPLESTORE_PATH");
    private static final String UPLOAD_PATH = System.getenv("SECURE_UPLOAD_TRIPLESTORE_PATH");
    private static final String DELETE_PATH = System.getenv("SECURE_DELETE_TRIPLESTORE_PATH");


    public static Response create(String storeID, Map<String, String> values) throws UnsupportedEncodingException {
        return sendRequest(storeID, new StringEntity(gson.toJson(values)), CREATE_PATH);
    }

    public static Response upload(String storeID, Map<String, String> values) throws UnsupportedEncodingException {
        return sendRequest(storeID, new StringEntity(gson.toJson(values)), UPLOAD_PATH);
    }

    public static Response delete(String storeID, List<String> trapdoors) throws UnsupportedEncodingException {
        return sendRequest(storeID, new StringEntity(gson.toJson(trapdoors)), DELETE_PATH);
    }

    public static Response delete(String storeID) {
        HttpDelete request = new HttpDelete(SECURE_TRIPLESTORE_URI + String.format(DELETE_PATH, storeID));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return Utils.buildResponse(client.execute(request));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Response sendRequest(String storeID, HttpEntity body, String path) {
        HttpPost request = new HttpPost(SECURE_TRIPLESTORE_URI + String.format(path, storeID));
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return Utils.buildResponse(client.execute(request));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
