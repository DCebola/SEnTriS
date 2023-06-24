package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EncryptedTriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("ENCRYPTED_TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("ENCRYPTED_TRIPLESTORE_DELETE_SOME_URI");
    private static final String SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_SEARCH_URI");

    private static final String UPDATE_URI = System.getenv("ENCRYPTED_TRIPLESTORE_UPDATE_URI");

    public static CloseableHttpResponse upload(HttpClient httpClient, String protocolVersion, String triplestoreID, Map<byte[], byte[]> values, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(UPLOAD_URI, protocolVersion, triplestoreID), ParsingUtils.mapOfBytesBytesToHttpEntity(values), accessToken);
    }

    public static CloseableHttpResponse search(HttpClient httpClient, String protocolVersion, String triplestoreID, List<byte[]> trapdoors, String accessToken) throws IOException {
        System.out.printf((SEARCH_URI) + "POST %n", protocolVersion, triplestoreID);
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(SEARCH_URI, protocolVersion, triplestoreID), ParsingUtils.bytesListToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteSome(HttpClient httpClient, String protocolVersion, String triplestoreID, Set<byte[]> trapdoors, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(DELETE_SOME_URI, protocolVersion, triplestoreID), ParsingUtils.bytesSetToHttpEntity(trapdoors), accessToken);
    }

    public static CloseableHttpResponse deleteAll(HttpClient httpClient, String protocolVersion, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_ALL_URI, protocolVersion, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse update(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID, List<byte[]> deletions, List<byte[]> uploads, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(UPDATE_URI, protocolVersion, triplestoreID), ParsingUtils.generateUpdateRequest(deletions, uploads), accessToken);
    }
}
