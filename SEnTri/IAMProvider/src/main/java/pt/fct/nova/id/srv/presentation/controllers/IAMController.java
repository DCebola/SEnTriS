package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.core.*;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.AccessRequest;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.RoleRequest;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.application.IAMStore.*;
import static pt.fct.nova.id.srv.presentation.Utils.extractAccessToken;
import static pt.fct.nova.id.srv.presentation.Utils.handleSessionException;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;

public class IAMController implements IdentityAndAccessManagementAPI {
    private static final String SUCCESSFUL_AUTH = "Successful authentication.";
    private static final String UNKNOWN_USER = "User not found.";
    private static final String INVALID_PASSWORD = "Password mismatch.";
    private static final String INTERNAL_ERROR = "Internal error.";
    private static final String USER_ALREADY_EXISTS = "User already exists.";
    private static final String SUCCESSFUL_USER_REGISTER = "Successful user registration.";
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
    private static final String ACCESS_ALLOWED = "Access allowed.";
    public static final String ACCESS_FORBIDDEN = "Access forbidden.";
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    public static final String UNKNOWN_OR_EXPIRED_TOKEN = "Token not found or expired.";
    private static final String SUCCESSFUL_ACCESS_TOKEN_DELETION = "Successful deletion of access token.";

    private static final String REQUEST_DECISION_MALFORMED = "Request decision malformed.";


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
    public Response revokeAccess(Cookie cookie, String username, AccessForm accessForm) {
        try {
            String issuerUsername = accessForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            String storeID = accessForm.getStoreID();
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerUsername.equals(username)) {
                if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuerUsername, storeID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }
            LocksClient.deleteUserStoreLock(username, storeID);
            String lockID = LocksClient.acquireStoreLock(issuerUsername, storeID);
            IAMStore.revokeAccess(storeID, username, accessForm.getWrite());
            LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
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
    public Response issueGrantAccessRequest(Cookie cookie, String username, AccessForm accessForm) {
        try {
            String issuerUsername = accessForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            String storeID = accessForm.getStoreID();
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuerUsername, storeID))) {
                if (username.equals(issuerUsername)) {
                    IAMStore.saveAccessRequest(new AccessRequest(username, storeID, accessForm.getWrite()));
                    return Response.ok(SUCCESSFUL_ACCESS_REQUEST_ISSUED).build();
                } else
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            } else {
                String lockID = LocksClient.acquireStoreLock(issuerUsername, storeID);
                IAMStore.grantAccess(storeID, username, accessForm.getWrite());
                LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
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
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED)) {
                if (username.equals(issuerUsername)) {
                    IAMStore.saveRoleRequest(new RoleRequest(username, roleForm.getRole()));
                    return Response.ok(SUCCESSFUL_ROLE_REQUEST_ISSUED).build();
                } else
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            } else if (!issuerRole.equals(ADMIN) && roleForm.getRole().equals(ADMIN)) {
                IAMStore.saveRoleRequest(new RoleRequest(username, roleForm.getRole()));
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
    public Response getPendingAccessRequests(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            if (!role.equals(ADMIN) && !role.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
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
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
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
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
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
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            return Response.ok(IAMStore.getPendingRoleRequest(requestID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processAccessRequest(Cookie cookie, String storeID, String requestID, RequestDecisionForm requestDecisionForm) {
        try {
            String issuerUsername = requestDecisionForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerRole.equals(ADMIN) && !issuerRole.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            String lockID = LocksClient.acquireStoreLock(issuerUsername, storeID);
            AccessRequest req = IAMStore.getPendingAccessRequest(requestID);
            if (req == null) {
                LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            }
            if (!storeID.equals(req.storeID())) {
                LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
                return Response.ok(REQUEST_DECISION_MALFORMED).status(BAD_REQUEST).build();
            }
            String username = req.target();
            if (!IAMStore.userExists(username)) {
                LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            }

            if (!IAMStore.checkIfOwns(username, storeID)) {
                if (requestDecisionForm.isAccept())
                    IAMStore.grantAccess(storeID, username, req.write());
                else
                    IAMStore.revokeAccess(storeID, username, req.write());
            }
            IAMStore.deleteAccessRequest(requestID);
            LocksClient.releaseStoreLock(issuerUsername, storeID, lockID);
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
    public Response processRoleRequest(Cookie cookie, String username, String requestID, RequestDecisionForm requestDecisionForm) {
        try {
            String issuerUsername = requestDecisionForm.getIssuer();
            Utils.authCheck(cookie, requestDecisionForm.getIssuer());
            Role issuerRole = IAMStore.getRole(issuerUsername);
            if (!issuerRole.equals(ADMIN) && !issuerRole.equals(MANAGER))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            String lockID = LocksClient.acquireUserLock(username);
            RoleRequest req = IAMStore.getPendingRoleRequest(requestID);
            if (req == null) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();
            }
            if (!username.equals(req.target())) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(REQUEST_DECISION_MALFORMED).status(BAD_REQUEST).build();
            }
            if (!IAMStore.userExists(req.target())) {
                LocksClient.releaseUserLock(username, lockID);
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            }
            if (requestDecisionForm.isAccept())
                IAMStore.setRole(req.target(), req.role());
            IAMStore.deleteAccessRequest(requestID);
            LocksClient.releaseUserLock(req.target(), lockID);
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
    public Response createStoreAccessPolicy(Cookie cookie, String username, StoreForm form) {
        try {
            String issuer = form.getIssuer();
            Utils.authCheck(cookie, issuer);
            Role issuerRole = IAMStore.getRole(issuer);

            if (issuer.equals(username) && issuerRole.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            if (!issuer.equals(username) && issuerRole.equals(BASIC) || issuerRole.equals(PRIVILEGED))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            if (IAMStore.getRole(username).equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String storeID = form.getStoreID();
            String lockID = LocksClient.acquireStoreLock(issuer, storeID);
            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                LocksClient.releaseStoreLock(issuer, storeID, lockID);
                return Response.ok(STORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            IAMStore.saveStoreAccessPolicy(storeID, username, new HashSet<>(), new HashSet<>());
            LocksClient.releaseStoreLock(issuer, storeID, lockID);
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
    public Response deleteStoreAccessPolicy(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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

            Role role = IAMStore.getRole(username);

            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, storeID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            IAMStore.deleteStoreAccessPolicy(storeID);
            LocksClient.releaseStoreLock(username, storeID, lockID);
            return Response.ok(SUCCESSFUL_STORE_ACCESS_POLICY_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getReadAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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
    public Response getWriteAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStore.checkIfUserHasWriteAccess(username, storeID) && !IAMStore.getRole(username).equals(ADMIN)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            }
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getOwnerAccess(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStore.checkIfOwns(username, storeID) && !IAMStore.getRole(username).equals(ADMIN))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response createAccessToken(Cookie cookie, String username, StoreForm form) {
        try {
            String issuer = form.getIssuer();
            String storeID = form.getStoreID();
            Utils.authCheck(cookie, issuer);
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(BAD_REQUEST).build();
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();

            Role issuerRole = IAMStore.getRole(issuer);
            if (!issuer.equals(username)) {
                if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuer, storeID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }
            Role role = IAMStore.getRole(issuer);

            if (!role.equals(ADMIN) &&
                    !IAMStore.checkIfUserHasReadAccess(username, storeID) &&
                    !IAMStore.checkIfUserHasWriteAccess(username, storeID) &&
                    !IAMStore.checkIfOwns(username, storeID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            return Response.ok(IAMStore.saveToken(username, storeID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response deleteAccessToken(Cookie cookie, String username, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String tokenOwner = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            Role role = IAMStore.getRole(username);
            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!username.equals(tokenOwner)) {
                if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, tokenStoreID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }
            IAMStore.deleteAccessToken(tokenID, token);
            return Response.ok(SUCCESSFUL_ACCESS_TOKEN_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }
}
