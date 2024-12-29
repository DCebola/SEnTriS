package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.DefaultStorage;
import pt.fct.nova.id.srv.application.RoleRequest;
import pt.fct.nova.id.srv.application.LocksClient;
import pt.fct.nova.id.srv.application.exceptions.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.crypto.PasswordLib;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.apis.UsersAPI;
import pt.fct.nova.id.srv.presentation.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidPasswordException;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;
import pt.fct.nova.id.srv.presentation.exceptions.UnknownUserException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static pt.fct.nova.id.srv.application.DefaultStorage.TOKEN_SESSION_FIELD;
import static pt.fct.nova.id.srv.application.DefaultStorage.TOKEN_USER_FIELD;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.dtos.Role.ADMIN;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoresController.*;

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
    private static final String ALREADY_HAS_ROLE = "User already has role.";

    @Override
    public Response auth(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            Utils.checkPassword(username, credentials.getPassword());
            NewCookie cookie = DefaultStorage.cacheSession(credentials.getUsername());
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
            if (DefaultStorage.userExists(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(USER_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            String passwordHash = Base64.encodeBase64URLSafeString(PasswordLib.hash(credentials.getPassword()));
            DefaultStorage.saveUser(username, passwordHash, BASIC);
            LocksClient.releaseUserLock(username, lockID);
            //TODO: Error messages should be equal... difference leak info about usernames/pass, triplestores
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
            if (DefaultStorage.checkIfOwnsAny(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(CAN_NOT_DELETE_STORE_OWNER).build();
            }
            DefaultStorage.deleteUser(username);
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
            if (!DefaultStorage.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            Role issuerRole = DefaultStorage.getRole(issuerUsername);
            Role role = roleForm.getRole();
            if (DefaultStorage.getRole(username).equals(role))
                return Response.ok(ALREADY_HAS_ROLE).status(BAD_REQUEST).build();
            if (issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED)) {
                if (username.equals(issuerUsername)) {
                    DefaultStorage.saveRoleRequest(username, role);
                    return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
                } else
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            } else {
                String lockID = LocksClient.acquireUserLock(username);
                DefaultStorage.setRole(username, role);
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(SUCCESSFUL_ROLE_GRANT).build();
            }
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
            Role role = DefaultStorage.getRole(username);
            if (!role.equals(ADMIN))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            return Response.ok(DefaultStorage.getPendingRoleRequests()).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String username, String requestID, RequestDecisionForm requestDecisionForm) {
        try {

            String targetUser = requestDecisionForm.getTarget();
            Utils.authCheck(cookie, username);
            Role issuerRole = DefaultStorage.getRole(username);
            if (!issuerRole.equals(ADMIN))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            if (!DefaultStorage.userExists(targetUser))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String lockID = LocksClient.acquireUserLock(targetUser);
            RoleRequest req = DefaultStorage.getPendingRoleRequest(requestID);
            if (req == null) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            }
            if (!targetUser.equals(req.username())) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_DECISION_MALFORMED).status(BAD_REQUEST).build();
            }
            if (requestDecisionForm.isAccept()) {
                if (DefaultStorage.getRole(targetUser).equals(req.role())) {
                    DefaultStorage.deleteRoleRequest(requestID);
                    LocksClient.releaseUserLock(targetUser, lockID);
                    return Response.ok(ALREADY_HAS_ROLE).status(BAD_REQUEST).build();
                }
                DefaultStorage.setRole(targetUser, req.role());
            }
            DefaultStorage.deleteRoleRequest(requestID);
            LocksClient.releaseUserLock(targetUser, lockID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            e.printStackTrace();
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response checkIfActive(List<String> authorizationHeaders) {
        String tokenID = extractAccessToken(authorizationHeaders);
        if (tokenID == null)
            return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
        Map<String, String> token = DefaultStorage.getToken(tokenID);
        if (token == null || token.isEmpty())
            return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

        String username = token.get(TOKEN_USER_FIELD);
        try {
            Utils.authCheck(token.get(TOKEN_SESSION_FIELD), username);
        } catch (SessionException e) {
            return handleSessionException(e);
        }
        return Response.ok(ACCESS_ALLOWED).build();
    }
}
