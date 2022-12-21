package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class IAMClient {
    private static final String CHECK_ACTIVE_URI = System.getenv("IAM_PROVIDER_CHECK_ACTIVE_URI");

    public static CloseableHttpResponse checkIfActive(HttpClient httpClient, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(CHECK_ACTIVE_URI), accessToken);
    }
}
