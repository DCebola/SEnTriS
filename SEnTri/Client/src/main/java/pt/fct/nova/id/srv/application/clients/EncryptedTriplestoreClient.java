package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class EncryptedTriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("ENCRYPTED_TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_SOME_URI");
    private static final String SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_SEARCH_URI");
    private static final String QUERY_URI = System.getenv("ENCRYPTED_TRIPLESTORE_QUERY_URI");

    public static CloseableHttpResponse upload(HttpClient httpClient, String storeID, Map<String, String> values, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(UPLOAD_URI, storeID), ParsingUtils.mapOfStringStringToHttpEntity(values), accessToken);
    }

    public static CloseableHttpResponse search(HttpClient httpClient, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(SEARCH_URI, storeID), ParsingUtils.stringListToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse query(HttpClient httpClient, String storeID, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(QUERY_URI, storeID), ParsingUtils.queryExecutionPlanToHttpEntity(plan), accessToken);
    }

    public static CloseableHttpResponse deleteSome(HttpClient httpClient, String storeID, List<String> trapdoors, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(DELETE_SOME_URI, storeID), ParsingUtils.stringListToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteAll(HttpClient httpClient, String storeID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_ALL_URI, storeID), accessToken);
    }


}
