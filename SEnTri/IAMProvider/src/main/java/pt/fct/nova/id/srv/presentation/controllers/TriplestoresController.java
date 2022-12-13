package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.jena.atlas.json.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
import pt.fct.nova.id.srv.application.AccessRequest;
import pt.fct.nova.id.srv.application.IAMStorage;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.TriplestoresAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.api.dtos.TriplestoreForm;
import pt.fct.nova.id.srv.presentation.api.dtos.UsersWithAccessResponse;
import pt.fct.nova.id.srv.presentation.exceptions.SessionException;

import java.util.*;

import static jakarta.ws.rs.core.Response.Status.*;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static pt.fct.nova.id.srv.application.IAMStorage.*;
import static pt.fct.nova.id.srv.presentation.Utils.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.*;
import static pt.fct.nova.id.srv.presentation.controllers.UsersController.UNKNOWN_USER;

@Path("/triplestores")
public class TriplestoresController implements TriplestoresAPI {
    private static final String TRIPLESTORE_ALREADY_EXISTS = "Store already exists.";
    public static final String UNKNOWN_TRIPLESTORE = "Store not found.";
    private static final String ALREADY_OWNS = "Already owns triplestore.";
    private static final String SUCCESSFUL_TRIPLESTORE_OWNER_CHANGE = "Successful change of triplestore ownership.";
    private static final String SUCCESSFUL_TRIPLESTORE_ACCESS_POLICY_CREATION = "Successful creation of triplestore access policy.";
    private static final String SUCCESSFUL_TRIPLESTORE_ACCESS_POLICY_DELETION = "Successful deletion of triplestore access policy.";
    private static final String SUCCESSFUL_ACCESS_REVOCATION = "Access revoked.";
    private static final String SUCCESSFUL_ACCESS_GRANT = "Access granted.";
    private static final String SUCCESSFUL_ACCESS_REQUEST_ISSUED = "Successful issued access request.";
    private static final String ALREADY_HAS_ACCESS = "Already has access to triplestore.";
    private static final String UNKNOWN_OR_EXPIRED_TOKEN = "Token not found or expired.";
    private static final String LOCK_NOT_FOUND = "Lock not found.";
    private static final String LOCK_NOT_FOUND_OR_EXPIRED = "Lock not found or expired.";
    private static final String SUCCESSFUL_ACCESS_TOKEN_DELETION = "Successful deletion of access token.";
    private static final String SUCCESSFUL_LOCK_RELEASE = "Successful lock release.";
    public static final String NO_ACCESS_TOKEN = "Malformed request: bearer token required.";
    private static final String ACCESS_ALLOWED = "Access allowed.";
    public static final String ACCESS_FORBIDDEN = "Access forbidden.";
    private static final String CANNOT_REVOKE_OWNER_OR_ADMIN_ACCESS = "Cannot revoke admin or owner access.";

    @Override
    public Response createTriplestoreAccessPolicy(Cookie cookie, TriplestoreForm form) {
        try {
            String owner = form.getIssuer();
            Utils.authCheck(cookie, owner);
            Role role = IAMStorage.getRole(owner);

            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String triplestoreID = form.getTriplestoreID();
            String lockID = LocksClient.acquireTriplestoreLock(owner, triplestoreID);
            if (IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                LocksClient.releaseTriplestoreLock(owner, triplestoreID, lockID);
                return Response.ok(TRIPLESTORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            IAMStorage.saveTriplestoreAccessPolicy(triplestoreID, owner, new HashSet<>(), new HashSet<>());
            LocksClient.releaseTriplestoreLock(owner, triplestoreID, lockID);
            return Response.ok(SUCCESSFUL_TRIPLESTORE_ACCESS_POLICY_CREATION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        } catch (TooManyLockRetriesException e) {
            return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response listTriplestores(Cookie cookie, String issuer, boolean write, boolean read, boolean owns) {
        try {
            Utils.authCheck(cookie, issuer);
            return Response.ok(IAMStorage.getTriplestores(issuer, write, read, owns)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response listUsersWithAccess(Cookie cookie, String triplestoreID, boolean write, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role issuerRole = IAMStorage.getRole(issuer);
            boolean isOwner = IAMStorage.checkIfOwns(issuer, triplestoreID);
            if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !isOwner))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            JSONObject responseBody = new JSONObject();
            Set<String> readUsers = IAMStorage.getUserWithReadAccess(triplestoreID);
            Set<String> writeUsers = new HashSet<>();
            if (write) {
                writeUsers = IAMStorage.getUserWithWriteAccess(triplestoreID);
                readUsers.removeAll(writeUsers);
            }
            String owner;
            if (!isOwner)
                owner = IAMStorage.getOwner(triplestoreID);
            else
                owner = issuer;
            responseBody.put("owner", owner)
                    .put("write", new JSONArray(writeUsers))
                    .put("read", new JSONArray(readUsers));
            return Response.ok(responseBody).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }


    @Override
    public Response changeTriplestoreOwner(Cookie cookie, String triplestoreID, String target, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            if (!IAMStorage.userExists(target))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role issuerRole = IAMStorage.getRole(issuer);

            if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(target, triplestoreID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String owner = IAMStorage.getOwner(triplestoreID);
            System.out.println("T: " + target);
            System.out.println("O: " + owner);

            if (target.equals(owner))
                return Response.ok(ALREADY_OWNS).status(BAD_REQUEST).build();

            Role role = IAMStorage.getRole(target);

            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();

            IAMStorage.updateTriplestoreOwner(triplestoreID, owner, target);
            return Response.ok(SUCCESSFUL_TRIPLESTORE_OWNER_CHANGE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response deleteTriplestoreAccessPolicy(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStorage.getRole(username);

            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(username, triplestoreID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            IAMStorage.deleteTriplestoreAccessPolicy(triplestoreID);
            IAMStorage.deleteAccessToken(tokenID, token);
            return Response.ok(SUCCESSFUL_TRIPLESTORE_ACCESS_POLICY_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response grantAccess(Cookie cookie, String triplestoreID, String target, boolean write, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            if (!IAMStorage.userExists(target))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            Role issuerRole = IAMStorage.getRole(issuer);

            if (issuer.equals(target)) {
                return Response.ok(ALREADY_HAS_ACCESS).status(BAD_REQUEST).build();
            } else if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(issuer, triplestoreID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();

            if (IAMStorage.getRole(target).equals(ADMIN) || IAMStorage.checkIfOwns(target, triplestoreID) ||
                    (IAMStorage.checkIfUserHasWriteAccess(target, triplestoreID) == write &&
                            IAMStorage.checkIfUserHasReadAccess(target, triplestoreID)))
                return Response.ok(ALREADY_HAS_ACCESS).status(BAD_REQUEST).build();

            IAMStorage.grantAccess(triplestoreID, target, write);
            return Response.ok(SUCCESSFUL_ACCESS_GRANT).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String triplestoreID, String target, boolean write, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            if (!IAMStorage.userExists(target))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            Role issuerRole = IAMStorage.getRole(issuer);

            if (!issuer.equals(target)) {
                if (issuerRole.equals(BASIC) || (issuerRole.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(issuer, triplestoreID)))
                    return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            }

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();

            if (IAMStorage.getRole(target).equals(ADMIN) || IAMStorage.checkIfOwns(target, triplestoreID))
                return Response.ok(CANNOT_REVOKE_OWNER_OR_ADMIN_ACCESS).status(BAD_REQUEST).build();

            LocksClient.deleteUserTriplestoreLock(target, triplestoreID);
            IAMStorage.revokeAccess(triplestoreID, target, write);
            return Response.ok(SUCCESSFUL_ACCESS_REVOCATION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response issueAccessRequest(Cookie cookie, String triplestoreID, String target, boolean write) {
        try {
            Utils.authCheck(cookie, target);
            if (!IAMStorage.storeAccessPolicyExists(triplestoreID))
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();

            Role role = IAMStorage.getRole(target);

            if (role.equals(ADMIN) || (role.equals(PRIVILEGED) && IAMStorage.checkIfOwns(target, triplestoreID)))
                return Response.ok(ALREADY_HAS_ACCESS).status(BAD_REQUEST).build();
            if (IAMStorage.checkIfUserHasWriteAccess(target, triplestoreID) == write &&
                    IAMStorage.checkIfUserHasReadAccess(target, triplestoreID))
                return Response.ok(ALREADY_HAS_ACCESS).status(BAD_REQUEST).build();

            IAMStorage.saveAccessRequest(triplestoreID, target, write);
            return Response.ok(SUCCESSFUL_ACCESS_REQUEST_ISSUED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response getPendingAccessRequests(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStorage.getRole(issuer);
            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(issuer, triplestoreID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            return Response.ok(IAMStorage.getPendingAccessRequests(triplestoreID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response processAccessRequest(Cookie cookie, String triplestoreID, String requestID, boolean decision, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String issuer = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, issuer);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStorage.getRole(issuer);
            if (role.equals(BASIC) || (role.equals(PRIVILEGED) && !IAMStorage.checkIfOwns(issuer, triplestoreID)))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            AccessRequest req = IAMStorage.getPendingAccessRequest(requestID);
            if (req == null)
                return Response.ok(REQUEST_NOT_FOUND).status(NOT_FOUND).build();

            String username = req.username();
            if (!IAMStorage.userExists(username))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();

            if (decision)
                IAMStorage.grantAccess(triplestoreID, username, req.write());

            IAMStorage.deleteAccessRequest(triplestoreID, requestID);
            return Response.ok(SUCCESSFUL_REQUEST_PROCESSING).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response createAccessToken(Cookie cookie, String triplestoreID, String target) {
        try {
            Utils.authCheck(cookie, target);
            System.out.println("Acquire access token: (" + target + ", " + triplestoreID + ")");

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID))
                return Response.ok(UNKNOWN_TRIPLESTORE).status(BAD_REQUEST).build();

            Role role = IAMStorage.getRole(target);

            if (!role.equals(ADMIN) &&
                    !IAMStorage.checkIfUserHasReadAccess(target, triplestoreID) &&
                    !IAMStorage.checkIfUserHasWriteAccess(target, triplestoreID) &&
                    !IAMStorage.checkIfOwns(target, triplestoreID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            return Response.ok(IAMStorage.saveToken(target, triplestoreID)).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response deleteAccessToken(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
            System.out.println("Trying to deleted access token [" + tokenID + "]: (" + triplestoreID + ")");
            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();
            System.out.println("Found token to delete [" + tokenID + "]: (" + triplestoreID + ")");
            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));
            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            IAMStorage.deleteAccessToken(tokenID, token);
            System.out.println("Deleted access token [" + tokenID + "]: (" + username + ", " + triplestoreID + ")");
            return Response.ok(SUCCESSFUL_ACCESS_TOKEN_DELETION).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response checkReadAccess(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
            System.out.println("Check read access [" + tokenID + "]:" + triplestoreID);
            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            System.out.println("Token: " + Arrays.toString(token.entrySet().toArray()));
            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStorage.checkIfUserHasReadAccess(username, triplestoreID) &&
                    !IAMStorage.checkIfUserHasWriteAccess(username, triplestoreID) &&
                    !IAMStorage.checkIfOwns(username, triplestoreID) &&
                    !IAMStorage.getRole(username).equals(ADMIN)) {
                IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            }
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }


    @Override
    public Response checkWriteAccess(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
            System.out.println("Check write access [" + tokenID + "]:" + triplestoreID);
            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            System.out.println("Token: " + Arrays.toString(token.entrySet().toArray()));
            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();

            if (!IAMStorage.checkIfUserHasWriteAccess(username, triplestoreID) &&
                    !IAMStorage.checkIfOwns(username, triplestoreID) &&
                    !IAMStorage.getRole(username).equals(ADMIN)) {
                IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();
            }
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response checkOwnerAccess(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();
            System.out.println("Check owner access [" + tokenID + "]:" + triplestoreID);
            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();
            System.out.println("Token: " + Arrays.toString(token.entrySet().toArray()));

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);
            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));
            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            if (!IAMStorage.checkIfOwns(username, triplestoreID) && !IAMStorage.getRole(username).equals(ADMIN))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(FORBIDDEN).build();
            else if (!LocksClient.checkIfTriplestoreLockExists(triplestoreID, lockID))
                return Response.ok(LOCK_NOT_FOUND_OR_EXPIRED).status(FORBIDDEN).build();
            return Response.ok(ACCESS_ALLOWED).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

    @Override
    public Response acquireTriplestoreLock(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            Role role = IAMStorage.getRole(username);
            if (!role.equals(ADMIN) && !IAMStorage.checkIfUserHasWriteAccess(username, triplestoreID) && !IAMStorage.checkIfOwns(username, triplestoreID))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();
            String lockID = LocksClient.acquireTriplestoreLock(username, triplestoreID);
            IAMStorage.addLockToToken(tokenID, lockID);
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
    public Response releaseTriplestoreLock(Cookie cookie, String triplestoreID, List<String> authorizationHeaders) {
        try {
            String tokenID = extractAccessToken(authorizationHeaders);
            if (tokenID == null)
                return Response.ok(NO_ACCESS_TOKEN).status(BAD_REQUEST).build();

            Map<String, String> token = IAMStorage.getToken(tokenID);
            if (token == null || token.isEmpty())
                return Response.ok(UNKNOWN_OR_EXPIRED_TOKEN).status(NOT_FOUND).build();

            String username = token.get(TOKEN_USER_FIELD);
            Utils.authCheck(cookie, username);

            String tokenStoreID = Objects.requireNonNull(token.get(TOKEN_TRIPLESTORE_FIELD));

            if (!IAMStorage.storeAccessPolicyExists(triplestoreID)) {
                if (triplestoreID.equals(tokenStoreID))
                    IAMStorage.deleteAccessToken(tokenID, token);
                return Response.ok(UNKNOWN_TRIPLESTORE).status(NOT_FOUND).build();
            }

            if (!tokenStoreID.equals(triplestoreID))
                return Response.ok(ACCESS_FORBIDDEN).status(FORBIDDEN).build();

            String lockID = token.get(TOKEN_LOCK_FIELD);
            if (lockID == null)
                return Response.ok(LOCK_NOT_FOUND).status(BAD_REQUEST).build();
            System.out.println("Release lock [" + tokenID + "]: (" + username + ", " + triplestoreID + ", " + lockID + ")");
            IAMStorage.deleteLockFromToken(tokenID);
            LocksClient.releaseTriplestoreLock(username, triplestoreID, lockID);
            return Response.ok(SUCCESSFUL_LOCK_RELEASE).build();
        } catch (SessionException e) {
            return handleSessionException(e);
        }
    }

}
