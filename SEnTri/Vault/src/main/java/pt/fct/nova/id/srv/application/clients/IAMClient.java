package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.Utils;

import java.io.IOException;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");
    private static final String GET_OWNER_ACCESS_PATH = System.getenv("GET_OWNER_ACCESS_PATH");
    private static final String GET_READ_ACCESS_PATH = System.getenv("GET_READ_ACCESS_PATH");
    private static final String GET_WRITE_ACCESS_PATH = System.getenv("GET_WRITE_ACCESS_PATH");

    public static CloseableHttpResponse hasReadAccess(Cookie cookie, String username, String storeID) throws IOException {
        return sendGETRequest(cookie, String.format(GET_READ_ACCESS_PATH, storeID, username));
    }

    public static CloseableHttpResponse hasWriteAccess(Cookie cookie, String username, String storeID) throws IOException {
        return sendGETRequest(cookie, String.format(GET_WRITE_ACCESS_PATH, storeID, username));
    }

    public static CloseableHttpResponse hasOwnerAccess(Cookie cookie, String username, String storeID) throws IOException {
        return sendGETRequest(cookie, String.format(GET_OWNER_ACCESS_PATH, storeID, username));
    }

    private static CloseableHttpResponse sendGETRequest(Cookie cookie, String path) throws IOException {
        HttpGet request = new HttpGet(IAM_PROVIDER_URI + path);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, Utils.generateContext(cookie.getValue()));
        }
    }


}
