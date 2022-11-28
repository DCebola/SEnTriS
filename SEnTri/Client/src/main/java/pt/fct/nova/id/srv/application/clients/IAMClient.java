package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.auth.AUTH;
import org.apache.http.client.methods.CloseableHttpResponse;

import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;

public class IAMClient {
    private static final String IAM_PROVIDER_URI = System.getenv("IAM_PROVIDER_URI");
    private static final String ACQUIRE_STORE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("ACQUIRE_LOCK_PATH"));
    private static final String RELEASE_STORE_LOCK_PATH = IAM_PROVIDER_URI.concat(System.getenv("RELEASE_LOCK_PATH"));
    private static final String CREATE_STORE_PATH = IAM_PROVIDER_URI.concat(System.getenv("CREATE_STORE_PATH"));
    private static final String GET_ACCESS_TOKEN_PATH = IAM_PROVIDER_URI.concat(System.getenv("CREATE_ACCESS_TOKEN_PATH"));
    private static final String AUTH_PATH = IAM_PROVIDER_URI.concat(System.getenv("AUTH_PATH"));
    private static final String REGISTER_USER_PATH = IAM_PROVIDER_URI.concat(System.getenv("REGISTER_USER_PATH"));
    private static final String DELETE_USER_PATH = IAM_PROVIDER_URI.concat(System.getenv("DELETE_USER_PATH"));
    private static final String ISSUE_ROLE_REQUEST_PATH = IAM_PROVIDER_URI.concat(System.getenv("ISSUE_ROLE_REQUEST_PATH"));

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

    public static CloseableHttpResponse authenticate(AuthForm credentialsForm) throws IOException {
        return HttpUtils.sendPOSTRequest(AUTH_PATH, ClientUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse registerUser(AuthForm credentialsForm) throws IOException {
        return HttpUtils.sendPOSTRequest(REGISTER_USER_PATH, ClientUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse deleteUser(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_USER_PATH, username));
    }

    public static CloseableHttpResponse issueUpgradeRequest(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(ISSUE_ROLE_REQUEST_PATH, username), ClientUtils.generatePrivilegeRoleRequest(username));
    }
}