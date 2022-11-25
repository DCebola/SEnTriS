package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.AccessRequest;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.StoresAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.api.dtos.StoreForm;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static pt.fct.nova.id.srv.application.IAMStore.*;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.controllers.UsersController.UNKNOWN_USER;

@Path("/stores")
public class StoresController implements StoresAPI {
    private static final String STORE_ALREADY_EXISTS = "Store already exists.";
    public static final String UNKNOWN_STORE = "Store not found.";
    private static final String ALREADY_OWNS = "Already owns store.";
    private static final String SUCCESSFUL_STORE_OWNER_CHANGE = "Successful change of store ownership.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_CREATION = "Successful creation of store access policy.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_DELETION = "Successful deletion of store access policy.";
    private static final String SUCCESSFUL_ACCESS_REVOCATION = "Access revoked.";
    private static final String SUCCESSFUL_ACCESS_GRANT = "Access granted.";
    private static final String SUCCESSFUL_ACCESS_REQUEST_ISSUED = "Successful issued access request.";
    private static final Object ALREADY_HAS_ACCESS = "Already has access to store.";
    private static final String UNKNOWN_OR_EXPIRED_TOKEN = "Token not found or expired.";
    private static final String LOCK_NOT_FOUND = "Lock not found.";
    private static final Object LOCK_NOT_FOUND_OR_EXPIRED = "Lock not found or expired.";
    private static final String SUCCESSFUL_ACCESS_TOKEN_DELETION = "Successful deletion of access token.";
    private static final String SUCCESSFUL_LOCK_RELEASE = "Successful lock release.";
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    private static final String ACCESS_ALLOWED = "Access allowed.";
    public static final String ACCESS_FORBIDDEN = "Access forbidden.";

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, StoreForm form) {
        try {
            String owner = form.getOwner();
            Utils.authCheck(cookie, owner);
            Role role = IAMStore.getRole(owner);

            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String storeID = form.getStoreID();
            String lockID = LocksClient.acquireStoreLock(owner, storeID);
            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                LocksClient.releaseStoreLock(owner, storeID, lockID);
                return Response.ok(STORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            IAMStore.saveStoreAccessPolicy(storeID, owner, new HashSet<>(), new HashSet<>());
            LocksClient.releaseStoreLock(owner, storeID, lockID);
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
    public Response listStores(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            return Response.ok(IAMStore.getStores()).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }


    @Override
    public Response changeStoreOwner(Cookie cookie, String username, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            if (username.equals(issuer))
                return Response.ok(ALREADY_OWNS).status(BAD_REQUEST).build();

            String storeID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }
            Role issuerRole = IAMStore.getRole(username);

            if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(username, storeID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            Role role = IAMStore.getRole(username);

            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            if (role.equals(ADMIN))
                IAMStore.updateStoreOwner(storeID, IAMStore.getOwner(storeID), username);
            else
                IAMStore.updateStoreOwner(storeID, issuer, username);
            return Response.ok(SUCCESSFUL_STORE_OWNER_CHANGE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
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
    public Response grantAccess(Cookie cookie, String storeID, String username, boolean write, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            Role issuerRole = IAMStore.getRole(issuer);

            if (!issuer.equals(username)) {
                if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuer, storeID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            IAMStore.grantAccess(storeID, username, write);
            return Response.ok(SUCCESSFUL_ACCESS_GRANT).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String storeID, String username, boolean write, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            Role issuerRole = IAMStore.getRole(issuer);

            if (!issuer.equals(username)) {
                if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuer, storeID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfStoreLockExists(storeID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            LocksClient.deleteUserStoreLock(username, storeID);
            IAMStore.revokeAccess(storeID, username, write);
            return Response.ok(SUCCESSFUL_ACCESS_REVOCATION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String storeID, AccessForm accessForm) {
        try {
            String username = accessForm.getUser();
            Utils.authCheck(cookie, username);
            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();

            Role role = IAMStore.getRole(username);

            if (role.equals(ADMIN) || (role.equals(PRIVILEGED) && IAMStore.checkIfOwns(username, storeID)))
                return Response.ok(ALREADY_HAS_ACCESS).build();
            if (IAMStore.checkIfUserHasWriteAccess(username, storeID) == accessForm.getWrite() &&
                    IAMStore.checkIfUserHasReadAccess(username, storeID))
                return Response.ok(ALREADY_HAS_ACCESS).build();

            IAMStore.saveAccessRequest(storeID, username, accessForm.getWrite());
            return Response.ok(SUCCESSFUL_ACCESS_REQUEST_ISSUED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getPendingAccessRequests(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStore.getRole(issuer);
            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuer, storeID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            return Response.ok(IAMStore.getPendingAccessRequests(storeID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processAccessRequest(Cookie cookie, String storeID, String requestID, boolean decision, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStore.getRole(issuer);
            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStore.checkIfOwns(issuer, storeID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            AccessRequest req = IAMStore.getPendingAccessRequest(requestID);
            if (req == null)
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();

            String username = req.user();
            if (!IAMStore.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            if (decision)
                IAMStore.grantAccess(storeID, username, req.write());

            IAMStore.deleteAccessRequest(storeID, requestID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response createAccessToken(Cookie cookie, String storeID, String username) {
        try {
            Utils.authCheck(cookie, username);

            if (!IAMStore.storeAccessPolicyExists(storeID))
                return Response.ok(UNKNOWN_STORE).status(BAD_REQUEST).build();

            Role role = IAMStore.getRole(username);

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
    public Response deleteAccessToken(Cookie cookie, String storeID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStore.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_STORE_FIELD));

            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                if (storeID.equals(tokenStoreID))
                    IAMStore.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(storeID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            IAMStore.deleteAccessToken(tokenID, token);
            return Response.ok(SUCCESSFUL_ACCESS_TOKEN_DELETION).build();
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

    @Override
    public Response acquireStoreLock(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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
    public Response releaseStoreLock(Cookie cookie, String storeID, List<String> authorizationHeaders) {
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
                return Response.ok(LOCK_NOT_FOUND).status(BAD_REQUEST).build();
            IAMStore.deleteLockFromToken(tokenID);
            LocksClient.releaseStoreLock(username, lockID, storeID);
            return Response.ok(SUCCESSFUL_LOCK_RELEASE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

}
