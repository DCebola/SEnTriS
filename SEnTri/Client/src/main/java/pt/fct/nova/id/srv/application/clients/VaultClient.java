package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String CREATE_SECRETS_URI = System.getenv("VAULT_CREATE_SECRETS_URI");
    private static final String GET_SECRETS_URI = System.getenv("VAULT_GET_SECRETS_URI");
    private static final String DELETE_SECRETS_URI = System.getenv("VAULT_DELETE_SECRETS_URI");

    public static CloseableHttpResponse saveProtocolSecrets(Cookie cookie, String storeID, Map<String, String> secrets, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, CREATE_SECRETS_URI, ClientUtils.generateSecretsForm(storeID, secrets), accessToken);
    }

    public static CloseableHttpResponse getProtocolSecrets(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(GET_SECRETS_URI, storeID), accessToken);
    }

    public static CloseableHttpResponse deleteProtocolSecrets(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_SECRETS_URI, storeID), accessToken);
    }

}
