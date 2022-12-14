package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

import static pt.fct.nova.id.srv.application.clients.HTTPUtils.sendGETRequest;

public class IAMClient {
    private static final String CHECK_TRIPLESTORE_OWNER_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_OWNER_ACCESS_URI");
    private static final String CHECK_TRIPLESTORE_READ_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_READ_ACCESS_URI");

    public static CloseableHttpResponse hasReadAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(httpClient, cookie, String.format(CHECK_TRIPLESTORE_READ_ACCESS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasOwnerAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(httpClient, cookie, String.format(CHECK_TRIPLESTORE_OWNER_ACCESS_URI, triplestoreID), accessToken);
    }
}
