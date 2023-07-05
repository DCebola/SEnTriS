package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class EncryptedTriplestoreV1Client extends EncryptedTriplestoreClient{
    private static final String PREPARE_SEARCH_URI = System.getenv("ENCRYPTED_TRIPLESTORE_PREPARE_SEARCH_URI");

    public static CloseableHttpResponse prepareSearch(CloseableHttpClient httpClient, String protocolVersion, String triplestoreID, List<String> trapdoors, String accessToken) throws IOException, URISyntaxException {
        System.out.printf((PREPARE_SEARCH_URI) + "POST %n", protocolVersion, triplestoreID);
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(PREPARE_SEARCH_URI, protocolVersion, triplestoreID), ParsingUtils.stringListToHttpEntity(trapdoors), accessToken);
    }

}
