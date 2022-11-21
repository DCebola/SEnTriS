package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Cookie;
import pt.fct.nova.id.srv.application.IAMStore;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidCookieException;
import pt.fct.nova.id.srv.presentation.exceptions.InvalidSessionException;
import pt.fct.nova.id.srv.presentation.exceptions.NoSessionFoundException;

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

}
