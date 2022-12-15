package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class IAMClient {
    private static final String CHECK_TRIPLESTORE_OWNER_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_OWNER_ACCESS_URI");
    private static final String CHECK_TRIPLESTORE_READ_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_READ_ACCESS_URI");
    private static final String CHECK_TRIPLESTORE_WRITE_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_WRITE_ACCESS_URI");

    public static CloseableHttpResponse hasReadAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_READ_ACCESS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasWriteAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_WRITE_ACCESS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasOwnerAccess(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(CHECK_TRIPLESTORE_OWNER_ACCESS_URI, triplestoreID), accessToken);
    }
}
