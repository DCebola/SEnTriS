package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.Map;


public class VaultClient {
    private static final String CREATE_SECRETS_URI = System.getenv("VAULT_CREATE_SECRETS_URI");
    private static final String GET_SECRETS_URI = System.getenv("VAULT_GET_SECRETS_URI");
    private static final String DELETE_SECRETS_URI = System.getenv("VAULT_DELETE_SECRETS_URI");

    public static CloseableHttpResponse saveProtocolV1Secrets(HttpClient httpClient, String triplestoreID, Map<String, byte[]> secrets, String accessToken) throws IOException {
        System.out.printf("POST" + (CREATE_SECRETS_URI) + "%n", triplestoreID);
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(CREATE_SECRETS_URI, triplestoreID), ParsingUtils.mapOfStringBytesToHttpEntity(secrets), accessToken);
    }
    public static CloseableHttpResponse saveProtocolSecrets(HttpClient httpClient, String triplestoreID, Map<byte[], byte[]> secrets, String accessToken) throws IOException {
        System.out.printf("POST" + (CREATE_SECRETS_URI) + "%n", triplestoreID);
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(CREATE_SECRETS_URI, triplestoreID), ParsingUtils.mapOfBytesBytesToHttpEntity(secrets), accessToken);
    }

    public static CloseableHttpResponse getProtocolSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        System.out.printf("GET" + (CREATE_SECRETS_URI) + "%n", triplestoreID);
        return HTTPUtils.sendGETRequest(httpClient, String.format(GET_SECRETS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse deleteProtocolSecrets(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_SECRETS_URI, triplestoreID), accessToken);
    }

}
