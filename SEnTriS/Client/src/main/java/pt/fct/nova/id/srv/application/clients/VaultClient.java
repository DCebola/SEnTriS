package pt.fct.nova.id.srv.application.clients;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String CREATE_SECRETS_URI = System.getenv("VAULT_CREATE_SECRETS_URI");
    private static final String GET_SECRETS_URI = System.getenv("VAULT_GET_SECRETS_URI");
    private static final String DELETE_SECRETS_URI = System.getenv("VAULT_DELETE_SECRETS_URI");

    public static CloseableHttpResponse saveSecrets(HttpClient httpClient, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(CREATE_SECRETS_URI, triplestoreID), ParsingUtils.mapOfStringsStringsToHttpEntity(secrets), accessToken);
    }

    public static CloseableHttpResponse getSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(GET_SECRETS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse deleteSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_SECRETS_URI, triplestoreID), accessToken);
    }

}
