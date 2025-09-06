package pt.fct.nova.id.srv.application.clients;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;

import java.io.IOException;

import static pt.fct.nova.id.srv.application.clients.HTTPUtils.sendGETRequest;

public class IAMClient {
    private static final String CHECK_TRIPLESTORE_OWNER_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_OWNER_ACCESS_URI");
    private static final String CHECK_TRIPLESTORE_READ_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_READ_ACCESS_URI");
    private static final String CHECK_TRIPLESTORE_WRITE_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_WRITE_ACCESS_URI");

    public static CloseableHttpResponse hasReadAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_READ_ACCESS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasWriteAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_WRITE_ACCESS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasOwnerAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_OWNER_ACCESS_URI, triplestoreID), accessToken);
    }
}
