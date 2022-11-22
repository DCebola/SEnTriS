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
import pt.fct.nova.id.srv.presentation.exceptions.InvalidCookieException;
import pt.fct.nova.id.srv.presentation.exceptions.NoSessionFoundException;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.BASIC;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.PRIVILEGED;

@Path("locks")
public class LocksController implements LocksAPI {

    private static final String INVALID_COOKIE = "Malformed cookie.";
    private static final String NO_SESSION_OR_EXPIRED = "User not authenticated or session has expired.";
    private static final String SESSION_VALUE_MISMATCH = "Invalid session for user.";
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final String UNKNOWN_STORE = "Store not found.";
    private static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    private static final String SUCCESSFUL_LOCK_RELEASE = "Successful lock release.";

    private Response handleSessionException(SessionException e) {
        if (e instanceof InvalidCookieException)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        else if (e instanceof NoSessionFoundException)
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        else
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
    }

    @Override
    public Response acquireStoreLock(Cookie cookie, String username, String storeID) {
        try {
            Utils.authCheck(cookie, username);
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();
            Role role = IAMStore.getRole(username);
            if ((role.equals(BASIC) && !IAMStore.checkIfUserHasWriteAccess(username, storeID)) ||
                    (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, storeID)) && !IAMStore.checkIfUserHasWriteAccess(username, storeID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            return Response.ok(LocksClient.acquireStoreLock(storeID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response releaseStoreLock(Cookie cookie, String lockID, String username, String storeID) {
        try {
            Utils.authCheck(cookie, username);
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();
            Role role = IAMStore.getRole(username);
            if ((role.equals(BASIC) && !IAMStore.checkIfUserHasWriteAccess(username, storeID)) ||
                    (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, storeID)) && !IAMStore.checkIfUserHasWriteAccess(username, storeID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            LocksClient.releaseStoreLock(lockID, storeID);
            return Response.ok(SUCCESSFUL_LOCK_RELEASE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

}
