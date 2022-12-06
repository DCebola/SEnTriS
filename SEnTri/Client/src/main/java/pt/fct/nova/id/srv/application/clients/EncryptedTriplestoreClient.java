package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.query.plans.QueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class EncryptedTriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("ENCRYPTED_TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_SOME_URI");
    private static final String SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_SEARCH_URI");
    private static final String QUERY_URI = System.getenv("ENCRYPTED_TRIPLESTORE_QUERY_URI");

    public static CloseableHttpResponse upload(Cookie cookie, String storeID, Map<String, String> values, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(UPLOAD_URI, storeID), ClientUtils.objectToHttpEntity(values), accessToken);
    }

    public static CloseableHttpResponse search(Cookie cookie, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(SEARCH_URI, storeID), ClientUtils.objectToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse query(Cookie cookie, String storeID, QueryExecutionPlan plan, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(QUERY_URI, storeID), ClientUtils.objectToHttpEntity(plan), accessToken);
    }

    public static CloseableHttpResponse deleteSome(Cookie cookie, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(DELETE_SOME_URI, storeID), ClientUtils.objectToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteAll(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_ALL_URI, storeID), accessToken);
    }
    
    

}
