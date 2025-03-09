package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.storage.redis.IAMStorage;
import pt.fct.nova.id.srv.application.crypto.PasswordLib;
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
    public static final String OPERATION_TIMEOUT = "Operation timeout.";

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
        if (!PasswordLib.verify(password, hash))
            throw new InvalidPasswordException();
    }

    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            if (val.contains(BEARER))
                return val.split(" ")[1];
        }
        return null;
    }

}
