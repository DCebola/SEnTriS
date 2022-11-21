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

    public static CloseableHttpResponse saveProtocolSecrets(Cookie cookie, String username, String storeID, Map<String, String> secrets) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie,
                String.format(CREATE_SECRETS_PATH, username, storeID),
                ClientUtils.secretsToHttpEntity(username, storeID, secrets));
    }

    public static CloseableHttpResponse getProtocolSecrets(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(GET_SECRETS_PATH, storeID, username));
    }

    public static CloseableHttpResponse deleteProtocolSecrets(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_SECRETS_PATH, username, storeID));
    }

}
