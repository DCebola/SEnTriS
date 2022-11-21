package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");
    private static final String ACQUIRE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("ACQUIRE_LOCK_PATH"));
    private static final String RELEASE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("RELEASE_LOCK_PATH"));
    private static final String CREATE_STORE_PATH = IAM_PROVIDER_URI.concat(System.getenv("CREATE_STORE_PATH"));
    private static final String DELETE_STORE_PATH = IAM_PROVIDER_URI.concat(System.getenv("DELETE_STORE_PATH"));

    public static CloseableHttpResponse acquireLock(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(ACQUIRE_LOCK_PATH, username, storeID));
    }

    public static void releaseLock(Cookie cookie, String username, String storeID, String lockID) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(RELEASE_LOCK_PATH, lockID, username, storeID));
        r.close();
    }

    public static CloseableHttpResponse createStore(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendEmptyPOSTRequest(cookie, String.format(CREATE_STORE_PATH, username, storeID));
    }

    public static void deleteStore(Cookie cookie, String username, String storeID) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, IAM_PROVIDER_URI + String.format(DELETE_STORE_PATH, username, storeID));
        r.close();
    }


}
