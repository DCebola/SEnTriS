package pt.fct.nova.id.srv.application.clients;


import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;

import java.io.IOException;

public class IAMClient {
    private static final String CHECK_ACTIVE_URI = System.getenv("IAM_PROVIDER_CHECK_ACTIVE_URI");

    public static CloseableHttpResponse checkIfActive(HttpClient httpClient, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(CHECK_ACTIVE_URI), accessToken);
    }
}
