package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.client.utils.URIBuilder;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.net.URISyntaxException;

public class IAMClient {
    private static final String AUTH_URI = System.getenv("IAM_PROVIDER_AUTH_URI");
    private static final String REGISTER_USER_URI = System.getenv("IAM_PROVIDER_REGISTER_USER_URI");
    private static final String DELETE_USER_URI = System.getenv("IAM_PROVIDER_DELETE_USER_URI");
    private static final String ISSUE_ROLE_REQUEST_URI = System.getenv("IAM_PROVIDER_ISSUE_ROLE_REQUEST_URI");
    private static final String CREATE_STORE_URI = System.getenv("IAM_PROVIDER_CREATE_STORE_URI");
    private static final String LIST_STORES_URI = System.getenv("IAM_PROVIDER_LIST_STORES_URI");
    private static final String DELETE_STORE_URI = System.getenv("IAM_PROVIDER_DELETE_STORE_URI");
    private static final String GRANT_ACCESS_URI = System.getenv("IAM_PROVIDER_GRANT_ACCESS_URI");
    private static final String REVOKE_ACCESS_URI = System.getenv("IAM_PROVIDER_REVOKE_ACCESS_URI");
    private static final String REQUEST_ACCESS_URI = System.getenv("IAM_PROVIDER_REQUEST_ACCESS_URI");
    private static final String GET_ACCESS_TOKEN_URI = System.getenv("IAM_PROVIDER_CREATE_ACCESS_TOKEN_URI");
    private static final String ACQUIRE_STORE_LOCK_URI = System.getenv("IAM_PROVIDER_ACQUIRE_LOCK_URI");
    private static final String RELEASE_STORE_LOCK_URI = System.getenv("IAM_PROVIDER_RELEASE_LOCK_URI");


    public static CloseableHttpResponse acquireStoreLock(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(ACQUIRE_STORE_LOCK_URI, storeID), accessToken);
    }

    public static void releaseStoreLock(Cookie cookie, String storeID, String accessToken) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(RELEASE_STORE_LOCK_URI, storeID), accessToken);
        r.close();
    }

    public static CloseableHttpResponse createStore(Cookie cookie, String storeID, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(CREATE_STORE_URI, username),
                ClientUtils.generateStoreForm(username, storeID));

    }

    public static CloseableHttpResponse deleteStore(Cookie cookie, String storeID, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_STORE_URI, storeID), accessToken);
    }

    public static CloseableHttpResponse getAccessToken(Cookie cookie, String username, String storeID) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(GET_ACCESS_TOKEN_URI, storeID, username));
    }

    public static CloseableHttpResponse authenticate(AuthForm credentialsForm) throws IOException {
        return HttpUtils.sendPOSTRequest(AUTH_URI, ClientUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse registerUser(AuthForm credentialsForm) throws IOException {
        return HttpUtils.sendPOSTRequest(REGISTER_USER_URI, ClientUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse deleteUser(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_USER_URI, username));
    }

    public static CloseableHttpResponse issueUpgradeRequest(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(ISSUE_ROLE_REQUEST_URI, username), ClientUtils.generatePrivilegeRoleRequest(username));
    }

    public static CloseableHttpResponse listStores(Cookie cookie, String username, boolean write, boolean read, boolean owns) throws URISyntaxException, IOException {
        return HttpUtils.sendGETRequest(cookie, new URIBuilder(String.format(LIST_STORES_URI, username))
                .addParameter("write", String.valueOf(write))
                .addParameter("read", String.valueOf(read))
                .addParameter("owns", String.valueOf(owns))
                .build());
    }

    public static CloseableHttpResponse requestAccess(Cookie cookie, String storeID, AccessForm form) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(REQUEST_ACCESS_URI, storeID), ClientUtils.accessFormToHttpEntity(form));
    }


    public static CloseableHttpResponse grantAccess(Cookie cookie, String storeID, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HttpUtils.sendPOSTRequest(cookie, new URIBuilder(String.format(GRANT_ACCESS_URI, storeID))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse revokeAccess(Cookie cookie, String storeID, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HttpUtils.sendDELETERequest(cookie, new URIBuilder(String.format(REVOKE_ACCESS_URI, storeID))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }


}