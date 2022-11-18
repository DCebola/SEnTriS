package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");

    private static final String GET_OWNER_ACCESS_PATH = System.getenv("GET_OWNER_ACCESS_PATH");
    private static final String GET_READ_ACCESS_PATH = System.getenv("GET_READ_ACCESS_PATH");
    private static final String GET_WRITE_ACCESS_PATH = System.getenv("GET_WRITE_ACCESS_PATH");

    public static CloseableHttpResponse hasReadAccess(String username, String storeID) throws IOException {
        return hasAccess(String.format(GET_READ_ACCESS_PATH, storeID, username));
    }

    public static CloseableHttpResponse hasWriteAccess(String username, String storeID) throws IOException {
        return hasAccess(String.format(GET_WRITE_ACCESS_PATH, storeID, username));
    }

    public static CloseableHttpResponse hasOwnerAccess(String username, String storeID) throws IOException {
        return hasAccess(String.format(GET_OWNER_ACCESS_PATH, storeID, username));
    }

    private static CloseableHttpResponse hasAccess(String path) throws IOException {
        HttpGet request = new HttpGet(IAM_PROVIDER_URI + path);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request);
        }
    }


}
