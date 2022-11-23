package pt.fct.nova.id.srv.application.clients;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Cookie;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class TriplestoreClient {

    private static final Gson gson = new Gson();
    private static final String SECURE_TRIPLESTORE_URI = System.getenv("SECURE_TRIPLESTORE_URI");
    private static final String UPLOAD_PATH = SECURE_TRIPLESTORE_URI.concat(System.getenv("SECURE_UPLOAD_TRIPLESTORE_PATH"));
    private static final String DELETE_PATH = SECURE_TRIPLESTORE_URI.concat(System.getenv("SECURE_DELETE_TRIPLESTORE_PATH"));
    private static final String SEARCH_PATH = SECURE_TRIPLESTORE_URI.concat(System.getenv("SECURE_SEARCH_TRIPLESTORE_PATH"));

    public static CloseableHttpResponse upload(Cookie cookie, String storeID, Map<String, String> values, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(UPLOAD_PATH, storeID), ClientUtils.objectToHttpEntity(values), accessToken);
    }

    public static CloseableHttpResponse search(Cookie cookie, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(SEARCH_PATH, storeID), ClientUtils.objectToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse delete(Cookie cookie, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(DELETE_PATH, storeID), ClientUtils.objectToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse delete(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_PATH, storeID), accessToken);
    }



}
