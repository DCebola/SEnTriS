package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String VAULT_URI = System.getenv("VAULT_URI");
    private static final String CREATE_SECRETS_PATH = VAULT_URI.concat(System.getenv("CREATE_SECRETS_PATH"));
    private static final String GET_SECRETS_PATH = VAULT_URI.concat(System.getenv("GET_SECRETS_PATH"));
    private static final String DELETE_SECRETS_PATH = VAULT_URI.concat(System.getenv("DELETE_SECRETS_PATH"));

    public static CloseableHttpResponse saveProtocolSecrets(Cookie cookie, Map<String, String> secrets, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(CREATE_SECRETS_PATH), ClientUtils.objectToHttpEntity(secrets), accessToken);
    }

    public static CloseableHttpResponse getProtocolSecrets(Cookie cookie, String accessToken) throws IOException {
        return HttpUtils.sendGETRequest(cookie, GET_SECRETS_PATH, accessToken);
    }

    public static CloseableHttpResponse deleteProtocolSecrets(Cookie cookie, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, DELETE_SECRETS_PATH, accessToken);
    }

}
