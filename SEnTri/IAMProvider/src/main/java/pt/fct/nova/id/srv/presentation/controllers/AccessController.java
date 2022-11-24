package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.AccessAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.api.dtos.StoreForm;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.IAMStore.*;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.controllers.StoresController.UNKNOWN_STORE;

@Path("/access")
public class AccessController implements AccessAPI {
    private static final String UNKNOWN_OR_EXPIRED_TOKEN = "Token not found or expired.";
    private static final String LOCK_NOT_FOUND = "Lock not found.";
    private static final Object LOCK_NOT_FOUND_OR_EXPIRED = "Lock not found or expired.";
    private static final String SUCCESSFUL_ACCESS_TOKEN_DELETION = "Successful deletion of access token.";
    public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    private static final String SUCCESSFUL_LOCK_RELEASE = "Successful lock release.";
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    private static final String ACCESS_ALLOWED = "Access allowed.";
    public static final String ACCESS_FORBIDDEN = "Access forbidden.";

    @Override
    public Response createAccessToken(Cookie cookie, StoreForm form) {
        try {
            String issuer = form.getIssuer();
            String storeID = form.getStoreID();
            Utils.authCheck(cookie, issuer);

            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();

            Role role = IAMStore.getRole(issuer);

            if (!role.equals(ADMIN) &&
                    !IAMStore.checkIfUserHasReadAccess(issuer, storeID) &&
                    !IAMStore.checkIfUserHasWriteAccess(issuer, storeID) &&
                    !IAMStore.checkIfOwns(issuer, storeID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            return Response.ok(IAMStore.saveToken(issuer, storeID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response deleteAccessToken(Cookie cookie, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            IAMStore.deleteAccessToken(tokenID, token);
            return Response.ok(SUCCESSFUL_ACCESS_TOKEN_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response acquireStoreLock(Cookie cookie, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            String storeID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !IAMStore.checkIfUserHasWriteAccess(username, storeID) && !IAMStore.checkIfOwns(username, storeID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            String lockID = LocksClient.acquireStoreLock(username, storeID);
            IAMStore.addLockToToken(tokenID, lockID);
            return Response.ok(lockID).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response releaseStoreLock(Cookie cookie, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String storeID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));
            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            IAMStore.deleteLockFromToken(tokenID);
            LocksClient.releaseStoreLock(username, lockID, storeID);
            return Response.ok(SUCCESSFUL_LOCK_RELEASE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response checkReadAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStore.checkIfUserHasReadAccess(username, storeID) &&
                    !IAMStore.checkIfUserHasWriteAccess(username, storeID) &&
                    !IAMStore.getRole(username).equals(ADMIN)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            }
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }


    @Override
    public Response checkWriteAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();

            if (!IAMStore.checkIfUserHasWriteAccess(username, storeID) &&
                    !IAMStore.checkIfOwns(username, storeID) &&
                    !IAMStore.getRole(username).equals(ADMIN)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            }
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response checkOwnerAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStore.checkIfOwns(username, storeID) && !IAMStore.getRole(username).equals(ADMIN))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

}
