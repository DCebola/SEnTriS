package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.clients.LockClient;
import pt.fct.nova.id.srv.application.clients.iam.IAMStore;
import pt.fct.nova.id.srv.application.clients.iam.StoreAccessPolicy;
import pt.fct.nova.id.srv.application.clients.iam.UserData;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.*;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static jakarta.ws.rs.core.Response.Status.*;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.BASIC;
import static pt.fct.nova.id.srv.presentation.api.dtos.Role.PRIVILEGED;

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
            if (IAMStore.getPassword(username) != null)
                return Response.ok(USER_ALREADY_EXISTS).status(BAD_REQUEST).build();
            String passwordHash = Base64.encodeBase64URLSafeString(PasswordUtils.hash(credentials.getPassword()));
            IAMStore.saveUser(
                    username,
                    passwordHash,
                    new UserData(Role.BASIC));
            return Response.ok(SUCCESSFUL_USER_REGISTER).build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response deleteUser(Cookie cookie, String username) {
        try {
            Utils.authCheck(cookie, username);
            IAMStore.deleteUser(username);
            return Response.ok(SUCCESSFUL_USER_DELETE).build();
        } catch (InvalidCookieException e) {
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        } catch (NoSessionFoundException e) {
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        } catch (InvalidSessionException e) {
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
        }
    }

    @Override
    public Response revokeAccess(Cookie cookie, String username, AccessPolicyForm accessPolicyForm) {
        try {
            String issuerUsername = accessPolicyForm.getIssuer();
            Utils.authCheck(cookie, issuerUsername);
            UserData issuer = IAMStore.getUserData(issuerUsername);
            UserData user = IAMStore.getUserData(username);
            String storeID = accessPolicyForm.getStoreID();
            String lockID = LockClient.acquireLock(storeID);
            if (lockID == null)
                return Response.ok(OPERATION_TIMEOUT).status(INTERNAL_SERVER_ERROR).build();
            StoreAccessPolicy storeAccessPolicy = IAMStore.getStoreAccessPolicy(storeID);

            if (user == null){
                LockClient.releaseLock(storeID, lockID);
                return Response.ok(UNKNOWN_USER).status(NOT_FOUND).build();
            }
            if (storeAccessPolicy == null) {
                LockClient.releaseLock(storeID, lockID);
                return Response.ok(UNKNOWN_STORE).status(NOT_FOUND).build();
            }
            assert issuer != null;
            Role role = issuer.getRole();

            if ((role.equals(PRIVILEGED) && !issuer.equals(user)) || (role.equals(BASIC) && !issuer.equals(user))) {
                LockClient.releaseLock(storeID, lockID);
                return Response.ok(INSUFFICIENT_PERMISSIONS).status(UNAUTHORIZED).build();
            }

            if (accessPolicyForm.getRead())
                storeAccessPolicy.getRead().remove(username);
            if (accessPolicyForm.getWrite())
                storeAccessPolicy.getWrite().remove(username);

            IAMStore.saveStoreAccessPolicy(storeID, storeAccessPolicy);
            LockClient.releaseLock(storeID, lockID);
            return Response.ok(SUCCESSFUL_ACCESS_REVOCATION).build();
        } catch (InvalidCookieException e) {
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        } catch (NoSessionFoundException e) {
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        } catch (InvalidSessionException e) {
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
        } catch (InterruptedException e) {
            return Response.ok(INTERNAL_ERROR).status(INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response issueGrantAccessRequest(Cookie cookie, String username, AccessPolicyForm accessPolicyForm) {
        return null;
    }

    @Override
    public Response issueGrantRoleRequest(Cookie cookie, String username, RoleForm roleForm) {
        return null;
    }

    @Override
    public Response getPendingAccessRequests(Cookie cookie, String username) {
        return null;
    }

    @Override
    public Response processAccessRequest(Cookie cookie, String requestID, RequestDecisionForm requestDecisionForm) {
        return null;
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie, String username) {
        return null;
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String requestID, RequestDecisionForm requestDecisionForm) {
        return null;
    }

    @Override
    public Response getStoreAccessPolicy(Cookie cookie, String username, String storeID) {
        return null;
    }

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, String username, String storeID, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response deleteStoreAccessPolicy(Cookie cookie, String username, String storeID) {
        return null;
    }

}
