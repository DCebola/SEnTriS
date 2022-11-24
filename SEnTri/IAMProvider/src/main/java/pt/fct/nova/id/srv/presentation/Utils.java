package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;
import pt.fct.nova.id.srv.presentation.exceptions.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;

public class Utils {
    public static final String INTERNAL_ERROR = "Internal error.";

    public static final String OPERATION_TIMEOUT = "Operation timeout.";
    private static final String INVALID_COOKIE = "Malformed cookie.";
    private static final String NO_SESSION_OR_EXPIRED = "User not authenticated or session has expired.";
    private static final String SESSION_VALUE_MISMATCH = "Invalid session for user.";

    public static void authCheck(Cookie cookie, String username) throws InvalidCookieException, NoSessionFoundException, InvalidSessionException {
        if (cookie == null || cookie.getValue() == null)
            throw new InvalidCookieException();
        String s = IAMStore.getSession(username);
        if (s == null || s.length() == 0)
            throw new NoSessionFoundException();
        if (!s.equals(cookie.getValue()))
            throw new InvalidSessionException();
    }

    public static void checkPassword(String username, String password) throws InvalidPasswordException, UnknownUserException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hash = Base64.decodeBase64(IAMStore.getPassword(username));
        if (hash == null)
            throw new UnknownUserException();
        if (!PasswordUtils.verify(password, hash))
            throw new InvalidPasswordException();
    }

    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            System.out.println(val);
            if (val.contains("Bearer"))
                return val;
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
