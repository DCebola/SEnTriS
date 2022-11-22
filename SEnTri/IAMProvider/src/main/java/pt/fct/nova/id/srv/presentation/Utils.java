package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Cookie;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidCookieException;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidSessionException;
import pt.fct.nova.id.srv.presentation.exceptions.NoSessionFoundException;

import java.util.List;

public class Utils {

    public static void authCheck(Cookie cookie, String username) throws InvalidCookieException, NoSessionFoundException, InvalidSessionException {
        if (cookie == null || cookie.getValue() == null)
            throw new InvalidCookieException();
        String s = IAMStore.getSession(username);
        if (s == null || s.length() == 0)
            throw new NoSessionFoundException();
        if (!s.equals(cookie.getValue()))
            throw new InvalidSessionException();
    }

    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            System.out.println(val);
            if (val.contains("Bearer"))
                return val;
        }
        return null;
    }

}
