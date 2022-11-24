package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.clients.exception.TooManyLockRetriesException;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.StoresAPI;
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
import static pt.fct.nova.id.srv.presentation.controllers.AccessController.*;
import static pt.fct.nova.id.srv.presentation.controllers.UsersController.UNKNOWN_USER;

@Path("/stores")
public class StoresController implements StoresAPI {
    private static final String STORE_ALREADY_EXISTS = "Store already exists.";
    public static final String UNKNOWN_STORE = "Store not found.";
    private static final String ALREADY_OWNS = "Already owns store.";
    private static final String SUCCESSFUL_STORE_OWNER_CHANGE = "Successful change of store ownership.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_CREATION = "Successful creation of store access policy.";
    private static final String SUCCESSFUL_STORE_ACCESS_POLICY_DELETION = "Successful deletion of store access policy.";

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, StoreForm form) {
        try {
            String issuer = form.getIssuer();
            Utils.authCheck(cookie, issuer);
            Role role = IAMStore.getRole(issuer);

            if (!IAMStore.userExists(issuer))
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            if (role.equals(BASIC))
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(FORBIDDEN).build();

            String storeID = form.getStoreID();
            String lockID = LocksClient.acquireStoreLock(issuer, storeID);
            if (!IAMStore.storeAccessPolicyExists(storeID)) {
                LocksClient.releaseStoreLock(issuer, storeID, lockID);
                return Response.ok(STORE_ALREADY_EXISTS).status(BAD_REQUEST).build();
            }
            IAMStore.saveStoreAccessPolicy(storeID, issuer, new HashSet<>(), new HashSet<>());
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
}
