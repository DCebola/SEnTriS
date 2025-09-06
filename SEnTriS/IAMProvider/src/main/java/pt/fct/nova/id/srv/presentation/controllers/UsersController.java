package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import pt.fct.nova.id.srv.application.storage.redis.IAMStorage;
import pt.fct.nova.id.srv.application.storage.RoleRequest;
import pt.fct.nova.id.srv.application.storage.redis.LocksClient;
import pt.fct.nova.id.srv.application.storage.redis.exceptions.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.crypto.PasswordLib;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.apis.UsersAPI;
import pt.fct.nova.id.srv.presentation.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.storage.redis.IAMStorage.TOKEN_SESSION_FIELD;
import static pt.fct.nova.id.srv.application.storage.redis.IAMStorage.TOKEN_USER_FIELD;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.dtos.Role.ADMIN;
import static pt.fct.nova.id.srv.presentation.controllers.TriplestoresController.*;

@Path("/users")
public class UsersController implements UsersAPI {
    private static final String SUCCESSFUL_AUTH = "Successful authentication.";
    private static final String SUCCESSFUL_USER_REGISTER = "Successful user registration.";
    private static final String SUCCESSFUL_USER_DELETE = "Successful user deletion.";
    private static final String SUCCESSFUL_ROLE_REQUEST_ISSUED = "Successful issued role request.";
    private static final String SUCCESSFUL_ROLE_GRANT = "Successful role grant.";
    private static final String REQUEST_DECISION_MALFORMED = "Request decision malformed.";
    public static final Response.Status LOCKED = Response.Status.fromStatusCode(423);

    @Override
    public Response auth(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            Utils.checkPassword(username, credentials.getPassword());
            NewCookie cookie = IAMStorage.cacheSession(credentials.getUsername());
            return Response.ok(SUCCESSFUL_AUTH).cookie(cookie).build();
        } catch (UnknownUserException | InvalidPasswordException e) {
            return Response.status(UNAUTHORIZED).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response registerUser(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            String lockID = LocksClient.acquireUserLock(username);
            if (IAMStorage.userExists(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.status(BAD_REQUEST).build();
            }
            String passwordHash = Base64.getUrlEncoder().encodeToString(PasswordLib.hash(credentials.getPassword()));
            IAMStorage.saveUser(username, passwordHash, BASIC);
            LocksClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_REGISTER).build();
        } catch (TooManyLockRetriesException e) {
            return Response.status(CONFLICT).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            String lockID = LocksClient.acquireUserLock(username);
            if (IAMStorage.checkIfOwnsAny(username)) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok().build();
            }
            IAMStorage.deleteUser(username);
            LocksClient.deleteAllUserLocks(username);
            LocksClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_DELETE).build();
        } catch (TooManyLockRetriesException e) {
            return Response.status(LOCKED).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueGrantRoleRequest(Cookie cookie, String username, RoleForm roleForm) {
        try {
            String issuerUsername = roleForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            if (!IAMStorage.userExists(username))
                return Response.status(NOT_FOUND).build();
            Role issuerRole = IAMStorage.getRole(issuerUsername);
            Role role = roleForm.getRole();
            if (IAMStorage.getRole(username).equals(role))
                return Response.ok(SUCCESSFUL_ROLE_GRANT).build();
            if (issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED)) {
                if (username.equals(issuerUsername)) {
                    IAMStorage.saveRoleRequest(username, role);
                    return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
                } else
                    return Response.status(UNAUTHORIZED).build();
            } else {
                String lockID = LocksClient.acquireUserLock(username);
                IAMStorage.setRole(username, role);
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(SUCCESSFUL_ROLE_GRANT).build();
            }
        } catch (TooManyLockRetriesException e) {
            return Response.status(LOCKED).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStorage.getRole(username);
            if (!role.equals(ADMIN))
                return Response.status(UNAUTHORIZED).build();
            return Response.ok(IAMStorage.getPendingRoleRequests()).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String username, String requestID, RequestDecisionForm requestDecisionForm) {
        try {

            String targetUser = requestDecisionForm.getTarget();
            Utils.authCheck(cookie, username);
            Role issuerRole = IAMStorage.getRole(username);
            if (!issuerRole.equals(ADMIN))
                return Response.status(UNAUTHORIZED).build();
            if (!IAMStorage.userExists(targetUser))
                return Response.status(NOT_FOUND).build();

            String lockID = LocksClient.acquireUserLock(targetUser);
            RoleRequest req = IAMStorage.getPendingRoleRequest(requestID);
            if (req == null) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            }
            if (!targetUser.equals(req.username())) {
                LocksClient.releaseUserLock(targetUser, lockID);
                return Response.ok(REQUEST_DECISION_MALFORMED).status(BAD_REQUEST).build();
            }
            if (requestDecisionForm.isAccept()) {
                if (IAMStorage.getRole(targetUser).equals(req.role())) {
                    IAMStorage.deleteRoleRequest(requestID);
                    LocksClient.releaseUserLock(targetUser, lockID);
                    return Response.ok(SUCCESSFUL_ROLE_GRANT).build();
                }
                IAMStorage.setRole(targetUser, req.role());
            }
            IAMStorage.deleteRoleRequest(requestID);
            LocksClient.releaseUserLock(targetUser, lockID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (TooManyLockRetriesException e) {
            return Response.status(LOCKED).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response checkIfActive(List<String> authorizationHeaders) {
        String tokenID = extractAccessToken(authorizationHeaders);
        if (tokenID == null)
            return Response.status(UNAUTHORIZED).build();
        Map<String, String> token = IAMStorage.getToken(tokenID);
        if (token == null || token.isEmpty())
            return Response.status(UNAUTHORIZED).build();

        String username = token.get(TOKEN_USER_FIELD);
        try {
            Utils.authCheck(token.get(TOKEN_SESSION_FIELD), username);
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
