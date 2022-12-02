package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

import static pt.fct.nova.id.srv.application.clients.HttpUtils.sendGETRequest;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");
    private static final String GET_TRIPLESTORE_OWNER_ACCESS_PATH = IAM_PROVIDER_URI.concat(System.getenv("GET_OWNER_ACCESS_PATH"));
    private static final String GET_TRIPLESTORE_READ_ACCESS_PATH = IAM_PROVIDER_URI.concat(System.getenv("GET_READ_ACCESS_PATH"));
    private static final String GET_TRIPLESTORE_WRITE_ACCESS_PATH = IAM_PROVIDER_URI.concat(System.getenv("GET_WRITE_ACCESS_PATH"));

    public static CloseableHttpResponse hasReadAccess(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(cookie, String.format(GET_TRIPLESTORE_READ_ACCESS_PATH, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasWriteAccess(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(cookie, String.format(GET_TRIPLESTORE_WRITE_ACCESS_PATH, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse hasOwnerAccess(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return sendGETRequest(cookie, String.format(GET_TRIPLESTORE_OWNER_ACCESS_PATH, triplestoreID), accessToken);
    }
}
