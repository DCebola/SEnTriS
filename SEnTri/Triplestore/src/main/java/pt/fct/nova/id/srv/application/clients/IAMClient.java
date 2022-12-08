package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

import static pt.fct.nova.id.srv.application.clients.HttpUtils.sendGETRequest;

public class IAMClient {
    private static final String IAM_PROVIDER_CHECK_STORE_OWNER_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_OWNER_ACCESS_URI");
    private static final String IAM_PROVIDER_CHECK_STORE_READ_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_READ_ACCESS_URI");
    private static final String IAM_PROVIDER_CHECK_STORE_WRITE_ACCESS_URI = System.getenv("IAM_PROVIDER_CHECK_WRITE_ACCESS_URI");

    public static CloseableHttpResponse hasReadAccess(Cookie cookie, String storeID, String accessToken) throws IOException {
        return sendGETRequest(cookie, String.format(IAM_PROVIDER_CHECK_STORE_READ_ACCESS_URI, storeID), accessToken);
    }

    public static CloseableHttpResponse hasWriteAccess(Cookie cookie, String storeID, String accessToken) throws IOException {
        System.out.printf((IAM_PROVIDER_CHECK_STORE_WRITE_ACCESS_URI) + "%n", storeID);
        return sendGETRequest(cookie, String.format(IAM_PROVIDER_CHECK_STORE_WRITE_ACCESS_URI, storeID), accessToken);
    }

    public static CloseableHttpResponse hasOwnerAccess(Cookie cookie, String storeID, String accessToken) throws IOException {
        return sendGETRequest(cookie, String.format(IAM_PROVIDER_CHECK_STORE_OWNER_ACCESS_URI, storeID), accessToken);
    }
}
