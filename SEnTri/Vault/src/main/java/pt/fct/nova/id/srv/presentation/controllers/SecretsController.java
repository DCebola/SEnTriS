package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.Vault;
import pt.fct.nova.id.srv.application.clients.HttpUtils;
import pt.fct.nova.id.srv.application.clients.IAMClient;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.api.SecretsAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.SecretsForm;

import java.io.IOException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

@Path("secrets")
public class SecretsController implements SecretsAPI {
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String STORE_ALREADY_EXISTS = "Store already exists.";
    private static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    private static final String SUCCESSFUL_SECRETS_CREATION = "Successful secrets creation.";
    private static final String SUCCESSFUL_SECRETS_DELETION = "Successful secrets deletion.";
    private static final String UNKNOWN_STORE = "Store not found.";
    private static final String OPERATION_TIMEOUT = "Operation timeout.";
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";

    @Override
    public Response createSecrets(Cookie cookie, SecretsForm form, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            String storeID = form.getStoreID();
            try (CloseableHttpResponse response = IAMClient.hasOwnerAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                if (!Boolean.parseBoolean(response.getEntity().toString()))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            }
            String lockID = LocksClient.acquireLock(storeID);
            if (Vault.exists(storeID)) {
                LocksClient.releaseLock(storeID, lockID);
                return Response.ok(STORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            Vault.saveSecrets(storeID, form.getSecrets());
            LocksClient.releaseLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_SECRETS_CREATION).build();
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (IOException | InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response getSecrets(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            try (CloseableHttpResponse response = IAMClient.hasReadAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                if (!Boolean.parseBoolean(response.getEntity().toString()))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            }
            if (!Vault.exists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            return Response.ok(Vault.getSecrets(storeID)).build();
        } catch (IOException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteSecrets(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String accessToken = extractAccessToken(authorizationHeaders);
            if (accessToken == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
            try (CloseableHttpResponse response = IAMClient.hasOwnerAccess(cookie, storeID, accessToken)) {
                if (response.getStatusLine().getStatusCode() != OK.getStatusCode())
                    return HttpUtils.buildResponse(response);
                if (!Boolean.parseBoolean(response.getEntity().toString()))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            }
            if (!Vault.exists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            String lockID = LocksClient.acquireLock(storeID);
            Vault.deleteSecrets(storeID);
            LocksClient.releaseLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_SECRETS_DELETION).build();
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (IOException | InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            if (val.contains("Bearer"))
                return val;
        }
        return null;
    }
}
