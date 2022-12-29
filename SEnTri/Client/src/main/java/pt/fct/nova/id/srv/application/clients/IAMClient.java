package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import org.apache.http.client.utils.URIBuilder;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.RequestDecisionForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.net.URISyntaxException;

public class IAMClient {
    private static final String AUTH_URI = System.getenv("IAM_PROVIDER_AUTH_URI");
    private static final String REGISTER_USER_URI = System.getenv("IAM_PROVIDER_REGISTER_USER_URI");
    private static final String DELETE_USER_URI = System.getenv("IAM_PROVIDER_DELETE_USER_URI");
    private static final String ISSUE_ROLE_REQUEST_URI = System.getenv("IAM_PROVIDER_ISSUE_ROLE_REQUEST_URI");
    private static final String LIST_PENDING_ROLE_REQUESTS_URI = System.getenv("IAM_PROVIDER_LIST_PENDING_ROLE_REQUESTS_URI");
    private static final String PROCESS_ROLE_REQUEST_URI = System.getenv("IAM_PROVIDER_PROCESS_ROLE_REQUEST_URI");
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
    private static final String LIST_USERS_WITH_ACCESS_URI = System.getenv("IAM_PROVIDER_LIST_USERS_WITH_ACCESS_URI");
    private static final String LIST_PENDING_ACCESS_REQUESTS_URI = System.getenv("IAM_PROVIDER_LIST_PENDING_ACCESS_REQUESTS_URI");
    private static final String PROCESS_ACCESS_REQUEST_URI = System.getenv("IAM_PROVIDER_PROCESS_ACCESS_REQUEST_URI");

    public static CloseableHttpResponse acquireTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, String.format(ACQUIRE_TRIPLESTORE_LOCK_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse releaseTriplestoreLock(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, String.format(RELEASE_TRIPLESTORE_LOCK_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse createTriplestore(HttpClient httpClient, Cookie cookie, String triplestoreID, String username) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, String.format(CREATE_TRIPLESTORE_URI, username),
                ParsingUtils.generateTriplestoreForm(username, triplestoreID));

    }

    public static CloseableHttpResponse deleteTriplestore(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, String.format(DELETE_TRIPLESTORE_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse createAccessToken(HttpClient httpClient, Cookie cookie, String username, String triplestoreID) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, String.format(CREATE_ACCESS_TOKEN_URI, triplestoreID, username));
    }

    public static CloseableHttpResponse deleteAccessToken(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, String.format(DELETE_ACCESS_TOKEN_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse authenticate(HttpClient httpClient, AuthForm credentialsForm) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, AUTH_URI, ParsingUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse registerUser(HttpClient httpClient, AuthForm credentialsForm) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, REGISTER_USER_URI, ParsingUtils.credentialsFormToHttpEntity(credentialsForm));
    }

    public static CloseableHttpResponse deleteUser(HttpClient httpClient, Cookie cookie, String username) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, String.format(DELETE_USER_URI, username));
    }

    public static CloseableHttpResponse issueUpgradeRequest(HttpClient httpClient, Cookie cookie, String username) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, String.format(ISSUE_ROLE_REQUEST_URI, username), ParsingUtils.generateRoleRequest(username, Role.PRIVILEGED));
    }

    public static CloseableHttpResponse issueDowngradeRequest(HttpClient httpClient, Cookie cookie, String username) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, String.format(ISSUE_ROLE_REQUEST_URI, username), ParsingUtils.generateRoleRequest(username, Role.BASIC));
    }

    public static CloseableHttpResponse listPendingRoleRequests(HttpClient httpClient, Cookie cookie, String username) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, cookie, String.format(LIST_PENDING_ROLE_REQUESTS_URI, username));
    }

    public static CloseableHttpResponse processRoleRequest(HttpClient httpClient, Cookie cookie, String username, String requestID, RequestDecisionForm decisionForm) throws URISyntaxException, IOException {
        return HTTPUtils.sendPUTRequest(httpClient, cookie, String.format(PROCESS_ROLE_REQUEST_URI, username, requestID), ParsingUtils.requestDecisionFormToHttpEntity(decisionForm));
    }

    public static CloseableHttpResponse listTriplestores(HttpClient httpClient, Cookie cookie, String issuer, boolean write, boolean read, boolean owns) throws URISyntaxException, IOException {
        return HTTPUtils.sendGETRequest(httpClient, cookie, new URIBuilder(String.format(LIST_TRIPLESTORES_URI, issuer))
                .addParameter("write", String.valueOf(write))
                .addParameter("read", String.valueOf(read))
                .addParameter("owns", String.valueOf(owns))
                .build());
    }

    public static CloseableHttpResponse requestAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, String issuer, boolean write) throws IOException, URISyntaxException {
        return HTTPUtils.sendPOSTRequest(httpClient, cookie, new URIBuilder(String.format(REQUEST_ACCESS_URI, triplestoreID, issuer))
                .addParameter("write", String.valueOf(write)).build());
    }


    public static CloseableHttpResponse grantAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, String username, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendPUTRequest(httpClient, cookie, new URIBuilder(String.format(GRANT_ACCESS_URI, triplestoreID, username))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse revokeAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, String username, boolean write, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendDELETERequest(httpClient, cookie, new URIBuilder(String.format(REVOKE_ACCESS_URI, triplestoreID, username))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse updateTriplestoreOwner(HttpClient httpClient, Cookie cookie, String triplestoreID, String target, String accessToken) throws IOException {
        return HTTPUtils.sendPUTRequest(httpClient, cookie, String.format(UPDATE_TRIPLESTORE_OWNER_URI, triplestoreID, target), accessToken);
    }

    public static CloseableHttpResponse listUsersWithAccess(HttpClient httpClient, Cookie cookie, String triplestoreID, boolean write, String accessToken) throws URISyntaxException, IOException {
        return HTTPUtils.sendGETRequest(httpClient, cookie, new URIBuilder(String.format(LIST_USERS_WITH_ACCESS_URI, triplestoreID))
                .addParameter("write", String.valueOf(write)).build(), accessToken);
    }

    public static CloseableHttpResponse listPendingAccessRequests(HttpClient httpClient, Cookie cookie, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, cookie, String.format(LIST_PENDING_ACCESS_REQUESTS_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse processAccessRequest(HttpClient httpClient, Cookie cookie, String triplestoreID, String requestID, boolean accept, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendPUTRequest(httpClient, cookie, new URIBuilder(String.format(PROCESS_ACCESS_REQUEST_URI, triplestoreID, requestID))
                .addParameter("accept", String.valueOf(accept)).build(), accessToken);
    }

}