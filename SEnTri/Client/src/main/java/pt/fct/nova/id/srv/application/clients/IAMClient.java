package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.client.utils.URIBuilder;
import org.apache.jena.base.Sys;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.controllers.ClientUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;

public class IAMClient {
    private static final String AUTH_URI = System.getenv("IAM_PROVIDER_AUTH_URI");
    private static final String REGISTER_USER_URI = System.getenv("IAM_PROVIDER_REGISTER_USER_URI");
    private static final String DELETE_USER_URI = System.getenv("IAM_PROVIDER_DELETE_USER_URI");
    private static final String ISSUE_ROLE_REQUEST_URI = System.getenv("IAM_PROVIDER_ISSUE_ROLE_REQUEST_URI");
    private static final String LIST_PENDING_ROLE_REQUESTS = System.getenv("IAM_PROVIDER_LIST_PENDING_ROLE_REQUESTS");
    private static final String PROCESS_ROLE_REQUEST = System.getenv("IAM_PROVIDER_PROCESS_ROLE_REQUEST");
    private static final String CREATE_TRIPLESTORE_URI = System.getenv("IAM_PROVIDER_CREATE_TRIPLESTORE_URI");
    private static final String LIST_TRIPLESTORES_URI = System.getenv("IAM_PROVIDER_LIST_TRIPLESTORES_URI");
    private static final String DELETE_TRIPLESTORE_URI = System.getenv("IAM_PROVIDER_DELETE_TRIPLESTORE_URI");
    private static final String GRANT_ACCESS_URI = System.getenv("IAM_PROVIDER_GRANT_ACCESS_URI");
    private static final String REVOKE_ACCESS_URI = System.getenv("IAM_PROVIDER_REVOKE_ACCESS_URI");
    private static final String REQUEST_ACCESS_URI = System.getenv("IAM_PROVIDER_REQUEST_ACCESS_URI");
    private static final String CREATE_ACCESS_TOKEN_URI = System.getenv("IAM_PROVIDER_CREATE_ACCESS_TOKEN_URI");
    private static final String DELETE_ACCESS_TOKEN_URI = System.getenv("IAM_PROVIDER_DELETE_ACCESS_TOKEN_URI");
    private static final String ACQUIRE_TRIPLESTORE_LOCK_URI = System.getenv("IAM_PROVIDER_ACQUIRE_TRIPLESTORE_LOCK_URI");
    private static final String RELEASE_TRIPLESTORE_LOCK_URI = System.getenv("IAM_PROVIDER_RELEASE_TRIPLESTORE_LOCK_URI");
    private static final String UPDATE_TRIPLESTORE_OWNER_URI = System.getenv("IAM_PROVIDER_UPDATE_TRIPLESTORE_OWNER_URI");
    private static final String LIST_USERS_WITH_ACCESS = System.getenv("IAM_PROVIDER_LIST_USERS_WITH_ACCESS");
    private static final String LIST_PENDING_ACCESS_REQUESTS = System.getenv("IAM_PROVIDER_LIST_PENDING_ACCESS_REQUESTS");
    private static final String PROCESS_ACCESS_REQUEST = System.getenv("IAM_PROVIDER_PROCESS_ACCESS_REQUEST");


    public static CloseableHttpResponse acquireTriplestoreLock(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(ACQUIRE_TRIPLESTORE_LOCK_URI, triplestoreID), accessToken);
    }

    public static void releaseTriplestoreLock(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(RELEASE_TRIPLESTORE_LOCK_URI, triplestoreID), accessToken);
        r.close();
    }

    public static CloseableHttpResponse createTriplestore(Cookie cookie, String triplestoreID, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(CREATE_TRIPLESTORE_URI, username),
                ClientUtils.generateTriplestoreForm(username, triplestoreID));

    }

    public static CloseableHttpResponse deleteTriplestore(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HttpUtils.sendDELETERequest(cookie, String.format(DELETE_TRIPLESTORE_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse createAccessToken(Cookie cookie, String username, String triplestoreID) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(CREATE_ACCESS_TOKEN_URI, triplestoreID, username));
    }

    public static void deleteAccessToken(Cookie cookie, String accessToken) throws IOException {
        CloseableHttpResponse r = HttpUtils.sendDELETERequest(cookie, String.format(DELETE_ACCESS_TOKEN_URI, accessToken));
        r.close();
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
        return HttpUtils.sendPOSTRequest(cookie, String.format(ISSUE_ROLE_REQUEST_URI, username), ClientUtils.generateRoleRequest(username, Role.PRIVILEGED));
    }

    public static CloseableHttpResponse issueDowngradeRequest(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(ISSUE_ROLE_REQUEST_URI, username), ClientUtils.generateRoleRequest(username, Role.BASIC));
    }

    public static CloseableHttpResponse listPendingRoleRequests(Cookie cookie, String username) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(LIST_PENDING_ROLE_REQUESTS, username));
    }

    public static CloseableHttpResponse processRoleRequest(Cookie cookie, String username, String requestID, boolean accept) throws URISyntaxException, IOException {
        return HttpUtils.sendPUTRequest(cookie, new URIBuilder(String.format(PROCESS_ROLE_REQUEST, username, requestID))
                .addParameter("accept", String.valueOf(accept)).build());
    }

    public static CloseableHttpResponse listTriplestores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) throws URISyntaxException, IOException {
        return HttpUtils.sendGETRequest(cookie, new URIBuilder(String.format(LIST_TRIPLESTORES_URI, issuer))
                .addParameter("write", String.valueOf(write))
                .addParameter("read", String.valueOf(read))
                .addParameter("owns", String.valueOf(owns))
                .build());
    }

    public static CloseableHttpResponse requestAccess(Cookie cookie, String triplestoreID, AccessForm form) throws IOException {
        return HttpUtils.sendPOSTRequest(cookie, String.format(REQUEST_ACCESS_URI, triplestoreID), ClientUtils.accessFormToHttpEntity(form));
    }


    public static CloseableHttpResponse grantAccess(Cookie cookie, String triplestoreID, String username, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HttpUtils.sendPUTRequest(cookie, new URIBuilder(String.format(GRANT_ACCESS_URI, triplestoreID, username))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse revokeAccess(Cookie cookie, String triplestoreID, String username, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HttpUtils.sendDELETERequest(cookie, new URIBuilder(String.format(REVOKE_ACCESS_URI, triplestoreID, username))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse updateTriplestoreOwner(Cookie cookie, String triplestoreID, String issuer, String accessToken) throws IOException {
        return HttpUtils.sendPUTRequest(cookie, String.format(UPDATE_TRIPLESTORE_OWNER_URI, triplestoreID, issuer), accessToken);
    }

    public static CloseableHttpResponse listUsersWithAccess(Cookie cookie, String triplestoreID, boolean write, String accessToken) throws URISyntaxException, IOException {
        return HttpUtils.sendGETRequest(cookie, new URIBuilder(String.format(LIST_USERS_WITH_ACCESS, triplestoreID))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse listPendingAccessRequests(Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HttpUtils.sendGETRequest(cookie, String.format(LIST_PENDING_ACCESS_REQUESTS, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse processAccessRequest(Cookie cookie, String triplestoreID, String requestID, boolean accept, String accessToken) throws IOException, URISyntaxException {
        return HttpUtils.sendPUTRequest(cookie, new URIBuilder(String.format(PROCESS_ACCESS_REQUEST, triplestoreID, requestID))
                .addParameter("accept", String.valueOf(accept)).build(), accessToken);
    }


}