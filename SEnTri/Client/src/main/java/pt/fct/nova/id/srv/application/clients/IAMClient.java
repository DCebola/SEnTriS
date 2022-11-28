package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");
    private static final String ACQUIRE_STORE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("ACQUIRE_LOCK_PATH"));
    private static final String RELEASE_STORE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("RELEASE_LOCK_PATH"));
    private static final String CREATE_STORE_PATH = IAM_PROVIDER_URI.concat(System.getenv("CREATE_STORE_PATH"));
    private static final String DELETE_STORE_PATH = IAM_PROVIDER_URI.concat(System.getenv("DELETE_STORE_PATH"));
    private static final String GET_ACCESS_TOKEN_PATH = IAM_PROVIDER_URI.concat(System.getenv("CREATE_ACCESS_TOKEN_PATH"));
    ;

    public static CloseableHttpResponse acquireStoreLock(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(ACQUIRE_STORE_LOCK_PATH, storeID), accessToken);
    }

    public static void releaseStoreLock(Cookie cookie, String storeID, String accessToken) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(RELEASE_STORE_LOCK_PATH, storeID), accessToken);
        r.close();
    }

    public static CloseableHttpResponse createStore(Cookie cookie, String storeID, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(CREATE_STORE_PATH, username),
                ClientUtils.generateStoreForm(username, storeID));
    }

    public static CloseableHttpResponse getAccessToken(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(GET_ACCESS_TOKEN_PATH, storeID, username));
    }

    public static void deleteStore(Cookie cookie, String storeID, String accessToken) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(DELETE_STORE_PATH, storeID), accessToken);
        r.close();
    }


}
