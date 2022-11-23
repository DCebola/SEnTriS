package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.LocksAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.IAMStore.*;
import static pt.fct.nova.id.srv.presentation.Utils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.Utils.handleSessionException;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.controllers.IAMController.*;

@Path("locks")
public class LocksController implements LocksAPI {
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final String UNKNOWN_STORE = "Store not found.";
    private static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    private static final String SUCCESSFUL_LOCK_RELEASE = "Successful lock release.";


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
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            IAMStore.deleteLockFromToken(tokenID);
            LocksClient.releaseStoreLock(username, lockID, storeID);
            return Response.ok(SUCCESSFUL_LOCK_RELEASE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

}
