package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.IAMStorage;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

public class Utils {
    private static final String BEARER = "Bearer";
    public static final String INTERNAL_ERROR = "Internal error.";
    public static final String SUCCESSFUL_REQUEST_PROCESSING = "Successful request processing.";
    public static final String REQUEST_NOT_FOUND = "Request not found.";
    public static final String INSUFFICIENT_PERMISSIONS = "Insufficient permissions to execute request.";
    public static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final String INVALID_COOKIE = "Malformed cookie.";
    private static final String NO_SESSION_OR_EXPIRED = "User not authenticated or session has expired.";
    private static final String SESSION_VALUE_MISMATCH = "Invalid session for user.";

    public static void authCheck(Cookie cookie, String username) throws InvalidCookieException, NoSessionFoundException, InvalidSessionException {
        if (cookie == null)
            throw new InvalidCookieException();
        authCheck(cookie.getValue(), username);
    }

    public static void authCheck(String cookieValue, String username) throws NoSessionFoundException, InvalidSessionException, InvalidCookieException {
        if (cookieValue == null)
            throw new InvalidCookieException();
        String session = IAMStorage.getSession(username);
        if (session == null || session.length() == 0)
            throw new NoSessionFoundException();
        if (!session.equals(cookieValue))
            throw new InvalidSessionException();
    }

    public static void checkPassword(String username, String password) throws InvalidPasswordException, UnknownUserException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hash = Base64.decodeBase64(IAMStorage.getPassword(username));
        if (hash == null)
            throw new UnknownUserException();
        if (!PasswordUtils.verify(password, hash))
            throw new InvalidPasswordException();
    }

    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            if (val.contains(BEARER))
                return val.split(" ")[1];
        }
        return null;
    }

    public static Response handleSessionException(SessionException e) {
        if (e instanceof InvalidCookieException)
            return Response.ok(INVALID_COOKIE).status(BAD_REQUEST).build();
        else if (e instanceof NoSessionFoundException)
            return Response.ok(NO_SESSION_OR_EXPIRED).status(NOT_FOUND).build();
        else
            return Response.ok(SESSION_VALUE_MISMATCH).status(UNAUTHORIZED).build();
    }

}
