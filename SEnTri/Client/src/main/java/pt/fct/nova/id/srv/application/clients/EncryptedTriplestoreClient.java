package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EncryptedTriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("ENCRYPTED_TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_SOME_URI");
    private static final String SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_SEARCH_URI");
    private static final String PREPARE_SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_PREPARE_SEARCH_URI");
    private static final String SWAP_URI = System.getenv("ENCRYPTED_TRIPLESTORE_SWAP_URI");

    public static CloseableHttpResponse upload(HttpClient httpClient, String triplestoreID, Map<String, String> values, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(UPLOAD_URI, triplestoreID), ParsingUtils.mapOfStringStringToHttpEntity(values), accessToken);
    }

    public static CloseableHttpResponse search(HttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(SEARCH_URI, triplestoreID), ParsingUtils.stringListToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse prepareSearch(CloseableHttpClient httpClient, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(PREPARE_SEARCH_URI, triplestoreID), ParsingUtils.stringListToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteSome(HttpClient httpClient, String triplestoreID, Set<String> trapdoors, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(DELETE_SOME_URI, triplestoreID), ParsingUtils.stringSetToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteAll(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_ALL_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse swap(CloseableHttpClient httpClient, String triplestoreID, Map<String, String> swaps, String accessToken) throws IOException {
        return HTTPUtils.sendPUTRequest(httpClient, String.format(SWAP_URI, triplestoreID), ParsingUtils.mapOfStringStringToHttpEntity(swaps), accessToken);
    }
}
