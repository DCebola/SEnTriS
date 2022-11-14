package pt.fct.nova.id.srv.presentation.controllers;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.clients.iam.IAMStore;
import pt.fct.nova.id.srv.application.clients.iam.UserAccessPolicy;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.Utils;
import pt.fct.nova.id.srv.presentation.api.IdentityAndAccessManagementAPI;
import pt.fct.nova.id.srv.presentation.api.dtos.AccessPolicyForm;
import pt.fct.nova.id.srv.presentation.api.dtos.AuthForm;
import pt.fct.nova.id.srv.presentation.api.dtos.Role;
import pt.fct.nova.id.srv.presentation.api.dtos.UserDTO;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import javax.naming.NoPermissionException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.Objects;

import static jakarta.ws.rs.core.Response.Status.*;

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
    private static final String SUCCESSFUL_USER_UPDATE = "Successful user update.";


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
                    new UserAccessPolicy(Role.BASIC));
            return Response.ok(SUCCESSFUL_USER_REGISTER).build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            return Response.ok(INTERNAL_ERROR).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Response processRoleRequest(Cookie cookie, String username, UserDTO user) {
        try {
            Utils.authCheck(cookie, username);
            if (Objects.requireNonNull(IAMStore.getUserAccessPolicy(username)).getRole() != Role.ADMIN)
                return Response.ok(INVALID_PERMISSIONS).status(UNAUTHORIZED).build();

            IAMStore.saveUser(username, new UserAccessPolicy());
            return Response.ok(SUCCESSFUL_USER_UPDATE).build();
        } catch (InvalidCookieException e) {
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        } catch (NoSessionFoundException e) {
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        } catch (InvalidSessionException e) {
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
        }
    }



    @Override
    public Response deleteUser(Cookie cookie, String username) {
        return null;
    }

    @Override
    public Response issueGrantAccessRequest(Cookie cookie, String username, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response revokeAccess(Cookie cookie, String username, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response getPendingRoleRequests(Cookie cookie) {
        return null;
    }

    @Override
    public Response getStoreAccessPolicy(Cookie cookie, String storeID) {
        return null;
    }

    @Override
    public Response createStoreAccessPolicy(Cookie cookie, String storeID, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response grantAccess(Cookie cookie, String storeID, AccessPolicyForm accessPolicy) {
        return null;
    }

    @Override
    public Response deleteStoreAccessPolicy(Cookie cookie, String storeID) {
        return null;
    }


}
