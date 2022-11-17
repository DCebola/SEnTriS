package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.clients.LockClient;
import pt.fct.nova.id.srv.application.clients.SecretsClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.clients.iam.*;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;

@Path("/iam")
public class IAMController implements IdentityAndAccessManagementAPI {
    private static final String SUCCESSFUL_AUTH = "Successful authentication.";
    private static final String UNKNOWN_USER = "User not found.";
    private static final String INVALID_PASSWORD = "Password mismatch.";
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String USER_ALREADY_EXISTS = "User already exists.";
    private static final String SUCCESSFUL_USER_REGISTER = "Successful user registration.";
    private static final String INVALID_COOKIE = "Malformed cookie.";
    private static final String NO_SESSION_OR_EXPIRED = "User not authenticated or session has expired.";
    private static final String SESSION_VALUE_MISMATCH = "Invalid session for user.";
    private static final String SUCCESSFUL_USER_DELETE = "Successful user deletion.";
    private static final String UNKNOWN_STORE = "Store not found.";
    private static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    private static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final String SUCCESSFUL_ACCESS_REVOCATION = "Successful access revocation.";
    private static final String SUCCESSFUL_ACCESS_REQUEST_ISSUED = "Successful issued access request.";
    private static final String SUCCESSFUL_ACCESS_GRANT = "Successful access grant.";
    private static final String SUCCESSFUL_ROLE_REQUEST_ISSUED = "Successful issued role request.";
    private static final String SUCCESSFUL_ROLE_GRANT = "Successful role grant.";
    private static final String REQUEST_NOT_FOUND = "Request not found.";
    private static final String SUCCESSFUL_REQUEST_PROCESSING = "Successful request processing.";
    private static final String STORE_ALREADY_EXISTS = "Store already exists.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_CREATION = "Successful creation of store access policy.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_DELETION = "Successful deletion of store access policy.";

    @Override
    public Response auth(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            checkPassword(username, credentials.getPassword());
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

    private void checkPassword(String username, String password) throws InvalidPasswordException, UnknownUserException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hash = Base64.decodeBase64(IAMStore.getPassword(username));
        if (hash == null)
            throw new UnknownUserException();
        if (!PasswordUtils.verify(password, hash))
            throw new InvalidPasswordException();
    }

    @Override
    public Response registerUser(AuthForm credentials) {
        try {
            String username = credentials.getUsername();
            String lockID = LockClient.acquireUserLock(username);
            if (IAMStore.userExists(username)) {
                LockClient.releaseUserLock(username, lockID);
                return Response.ok(USER_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            String passwordHash = Base64.encodeBase64URLSafeString(PasswordUtils.hash(credentials.getPassword()));
            IAMStore.saveUser(username, passwordHash, BASIC, new HashSet<>());
            LockClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_REGISTER).build();
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            String lockID = LockClient.acquireUserLock(username);
            IAMStore.deleteUser(username);
            LockClient.releaseUserLock(username, lockID);
            return Response.ok(SUCCESSFUL_USER_DELETE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Response handleSessionException(SessionException e) {
        if (e instanceof InvalidCookieException)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        else if (e instanceof NoSessionFoundException)
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        else
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
    }

    @Override
    public Response revokeAccess(Cookie cookie, String username, AccessPolicyForm accessPolicyForm) {
        try {
            String issuerUsername = accessPolicyForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            String storeID = accessPolicyForm.getStoreID();
            if (IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            if (IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String lockID = LockClient.acquireStoreAccessPolicyLock(storeID);
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerUsername.equals(username) && (issuerRole.equals(PRIVILEGED) || issuerRole.equals(BASIC))) {
                LockClient.releaseStoreAccessPolicyLock(storeID, lockID);
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            }
            IAMStore.revokeAccess(storeID, username, accessPolicyForm.getRead(), accessPolicyForm.getWrite());
            LockClient.releaseStoreAccessPolicyLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_ACCESS_REVOCATION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueGrantAccessRequest(Cookie cookie, String username, AccessPolicyForm accessPolicyForm) {
        try {
            String issuerUsername = accessPolicyForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            String storeID = accessPolicyForm.getStoreID();
            if (IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            if (IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            String lockID = LockClient.acquireStoreAccessPolicyLock(storeID);
            Role issuerRole = IAMStore.getRole(issuerUsername);

            if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuerUsername, storeID))) {
                if (username.equals(issuerUsername)) {
                    IAMStore.saveAccessRequest(new AccessRequest(username, storeID, accessPolicyForm.getRead(), accessPolicyForm.getWrite()));
                    LockClient.releaseStoreAccessPolicyLock(storeID, lockID);
                    return Response.ok(SUCCESSFUL_ACCESS_REQUEST_ISSUED).build();
                } else {
                    LockClient.releaseStoreAccessPolicyLock(storeID, lockID);
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
                }
            } else {
                IAMStore.grantAccess(storeID, username, accessPolicyForm.getRead(), accessPolicyForm.getWrite());
                LockClient.releaseStoreAccessPolicyLock(storeID, lockID);
                return Response.ok(SUCCESSFUL_ACCESS_GRANT).build();
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
    public Response issueGrantRoleRequest(Cookie cookie, String username, RoleForm roleForm) {
        try {
            String issuerUsername = roleForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            if (IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            String lockID = LockClient.acquireUserLock(username);
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED)) {
                if (username.equals(issuerUsername)) {
                    IAMStore.saveRoleRequest(new RoleRequest(username, roleForm.getRole()));
                    LockClient.releaseUserLock(username, lockID);
                    return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
                } else {
                    LockClient.releaseUserLock(username, lockID);
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
                }
            } else if (!issuerRole.equals(ADMIN) && roleForm.getRole().equals(ADMIN)) {
                IAMStore.saveRoleRequest(new RoleRequest(username, roleForm.getRole()));
                LockClient.releaseUserLock(username, lockID);
                return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
            }
            IAMStore.setRole(username, roleForm.getRole());
            LockClient.releaseUserLock(username, lockID);
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
    public Response getPendingAccessRequests(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !role.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            return Response.ok(IAMStore.getPendingAccessRequests()).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !role.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            return Response.ok(IAMStore.getPendingRoleRequests()).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getPendingAccessRequest(Cookie cookie, String username, String requestID) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !role.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            return Response.ok(IAMStore.getPendingAccessRequest(requestID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getPendingRoleRequest(Cookie cookie, String username, String requestID) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !role.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            return Response.ok(IAMStore.getPendingRoleRequest(requestID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processAccessRequest(Cookie cookie, String requestID, RequestDecisionForm requestDecisionForm) {
        try {
            String issuerUsername = requestDecisionForm.getIssuer();
            Utils.authCheck(cookie, requestDecisionForm.getIssuer());
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerRole.equals(ADMIN) && !issuerRole.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            AccessRequest req = IAMStore.getPendingAccessRequest(requestID);
            if (req == null)
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            if (IAMStore.storeAccessPolicyExists(req.storeID()))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            if (IAMStore.userExists(req.target()))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String lockID = LockClient.acquireStoreAccessPolicyLock(req.storeID());

            if (requestDecisionForm.isAccept())
                IAMStore.grantAccess(req.storeID(), req.target(), req.read(), req.write());
            else
                IAMStore.revokeAccess(req.storeID(), req.target(), req.read(), req.write());
            IAMStore.deleteAccessRequest(requestID);
            LockClient.releaseStoreAccessPolicyLock(req.storeID(), lockID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String requestID, RequestDecisionForm requestDecisionForm) {
        try {
            String issuerUsername = requestDecisionForm.getIssuer();
            Utils.authCheck(cookie, requestDecisionForm.getIssuer());
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerRole.equals(ADMIN) && !issuerRole.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            RoleRequest req = IAMStore.getPendingRoleRequest(requestID);
            if (req == null)
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            if (IAMStore.userExists(req.target()))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String lockID = LockClient.acquireUserLock(req.target());

            if (requestDecisionForm.isAccept())
                IAMStore.setRole(req.target(), req.role());
            IAMStore.deleteAccessRequest(requestID);
            LockClient.releaseUserLock(req.target(), lockID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, String username, String storeID) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            String lockID = LockClient.acquireStoreLock(storeID);
            if (SecretsClient.exists(storeID)) {
                LockClient.releaseStoreLock(storeID, lockID);
                return Response.ok(STORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            IAMStore.saveStoreAccessPolicy(storeID, username, new HashSet<>(), new HashSet<>());
            LockClient.releaseStoreLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_STORE_ACCESS_POLICY_CREATION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteStoreAccessPolicy(Cookie cookie, String username, String storeID) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();
            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, storeID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            String lockID = LockClient.acquireStoreLock(storeID);
            IAMStore.deleteStoreAccessPolicy(storeID);
            LockClient.releaseStoreLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_STORE_ACCESS_POLICY_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
