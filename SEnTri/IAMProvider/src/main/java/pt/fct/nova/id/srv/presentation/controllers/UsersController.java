package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.RoleRequest;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.UsersAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidPasswordException;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownUserException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.ADMIN;

@Path("/users")
public class UsersController implements UsersAPI {
    private static final String SUCCESSFUL_AUTH = "Successful authentication.";
    private static final String INVALID_PASSWORD = "Password mismatch.";
    public static final String UNKNOWN_USER = "User not found.";
    private static final String USER_ALREADY_EXISTS = "User already exists.";
    private static final String CAN_NOT_DELETE_STORE_OWNER = "Can not delete store owners.";
    private static final String SUCCESSFUL_USER_REGISTER = "Successful user registration.";
    private static final String SUCCESSFUL_USER_DELETE = "Successful user deletion.";
    private static final String SUCCESSFUL_ROLE_REQUEST_ISSUED = "Successful issued role request.";
    private static final String SUCCESSFUL_ROLE_GRANT = "Successful role grant.";
    private static final String REQUEST_DECISION_MALFORMED = "Request decision malformed.";

    @Override
    public Response auth(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            Utils.checkPassword(username, credentials.getPassword());
            NewCookie cookie = IAMStore.cacheSession(credentials.getUsername());
            return Response.ok(SUCCESSFUL_AUTH).cookie(cookie).build();
        } catch (UnknownUserException e) {
            return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
        } catch (InvalidPasswordException e) {
            return Response.ok(INVALID_PASSWORD).status(UNAUTHORIZED).build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response registerUser(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            String lockID = LocksClient.acquireUserLock(username);
            if (IAMStore.userExists(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(USER_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            String passwordHash = Base64.encodeBase64URLSafeString(PasswordUtils.hash(credentials.getPassword()));
            IAMStore.saveUser(username, passwordHash, BASIC);
            LocksClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_REGISTER).build();
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            String lockID = LocksClient.acquireUserLock(username);
            IAMStore.deleteUser(username);
            if (IAMStore.checkIfOwnsAny(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(CAN_NOT_DELETE_STORE_OWNER).build();
            }
            LocksClient.deleteAllUserLocks(username);
            LocksClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_DELETE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueGrantRoleRequest(Cookie cookie, String username, RoleForm roleForm) {
        try {
            String issuerUsername = roleForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED)) {
                if (username.equals(issuerUsername)) {
                    IAMStore.saveRoleRequest(username, roleForm.getRole());
                    return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
                } else
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            } else if (!issuerRole.equals(ADMIN) && roleForm.getRole().equals(ADMIN)) {
                IAMStore.saveRoleRequest(username, roleForm.getRole());
                return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
            }
            String lockID = LocksClient.acquireUserLock(username);
            IAMStore.setRole(username, roleForm.getRole());
            LocksClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_ROLE_GRANT).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            return Response.ok(IAMStore.getPendingRoleRequests()).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String username, String requestID, RequestDecisionForm requestDecisionForm) {
        try {
            String targetUser = requestDecisionForm.getTarget();
            Utils.authCheck(cookie, username);
            Role issuerRole = IAMStore.getRole(username);
            if (!issuerRole.equals(ADMIN))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            if (!IAMStore.userExists(targetUser))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String lockID = LocksClient.acquireUserLock(targetUser);
            RoleRequest req = IAMStore.getPendingRoleRequest(requestID);
            if (req == null) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            }
            if (!targetUser.equals(req.user())) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_DECISION_MALFORMED).status(BAD_REQUEST).build();
            }
            if (requestDecisionForm.isAccept())
                IAMStore.setRole(targetUser, req.role());
            IAMStore.deleteRoleRequest(requestID);
            LocksClient.releaseUserLock(targetUser, lockID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
