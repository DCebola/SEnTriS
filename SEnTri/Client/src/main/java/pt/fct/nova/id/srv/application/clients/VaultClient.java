package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String CREATE_SECRETS_URI = System.getenv("VAULT_CREATE_SECRETS_URI");
    private static final String GET_SECRETS_URI = System.getenv("VAULT_GET_SECRETS_URI");
    private static final String DELETE_SECRETS_URI = System.getenv("VAULT_DELETE_SECRETS_URI");

    public static CloseableHttpResponse saveProtocolSecrets(HttpClient httpClient, Cookie cookie, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, CREATE_SECRETS_URI, ParsingUtils.generateSecretsForm(triplestoreID, secrets), accessToken);
    }

    public static CloseableHttpResponse getProtocolSecrets(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, cookie, String.format(GET_SECRETS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse deleteProtocolSecrets(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, String.format(DELETE_SECRETS_URI, triplestoreID), accessToken);
    }

}
