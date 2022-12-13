package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.HttpResponse;

import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String CREATE_SECRETS_URI = System.getenv("VAULT_CREATE_SECRETS_URI");
    private static final String GET_SECRETS_URI = System.getenv("VAULT_GET_SECRETS_URI");
    private static final String DELETE_SECRETS_URI = System.getenv("VAULT_DELETE_SECRETS_URI");

    public static HttpResponse saveProtocolSecrets(Cookie cookie, String triplestoreID, Map<String, String> secrets, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(cookie, CREATE_SECRETS_URI, ParsingUtils.generateSecretsForm(triplestoreID, secrets), accessToken);
    }

    public static HttpResponse getProtocolSecrets(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(cookie, String.format(GET_SECRETS_URI, triplestoreID), accessToken);
    }

    public static HttpResponse deleteProtocolSecrets(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(cookie, String.format(DELETE_SECRETS_URI, triplestoreID), accessToken);
    }

}
