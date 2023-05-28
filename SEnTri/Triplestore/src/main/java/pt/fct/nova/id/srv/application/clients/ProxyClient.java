package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.List;

public class ProxyClient {
    private static final String PREPARE_SEARCH_URI = System.getenv("PROXY_PREPARE_SEARCH_URI");

    public static CloseableHttpResponse prepareSearch(CloseableHttpClient httpClient, String protocolVersion, List<byte[]> searchResults, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(PREPARE_SEARCH_URI, protocolVersion), ParsingUtils.listToHttpEntity(searchResults), accessToken);
    }
}
