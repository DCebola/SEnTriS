package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

import static pt.fct.nova.id.srv.application.clients.HTTPUtils.sendGETRequest;

public class IAMClient {
    private static final String CHECK_ACTIVE_URI = System.getenv("IAM_PROVIDER_CHECK_ACTIVE_URI");

    public static CloseableHttpResponse checkIfActive(HttpClient httpClient, String accessToken) throws IOException {
        return sendGETRequest(httpClient, String.format(CHECK_ACTIVE_URI), accessToken);
    }
}
