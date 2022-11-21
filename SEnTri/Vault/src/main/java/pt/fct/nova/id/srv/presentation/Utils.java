package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Utils {

    public static final String COOKIE_PARAM = "session";
    private static final String COOKIE_LIFETIME = System.getenv("COOKIE_LIFETIME");

    public static Response buildResponse(CloseableHttpResponse response) throws IOException {
        return Response.ok(EntityUtils.toString(response.getEntity())).status(response.getStatusLine().getStatusCode()).build();
    }

    public static HttpContext generateContext(String cookieValue) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = new BasicHttpContext();
        cookieStore.addCookie(Utils.createApacheCookie(cookieValue));
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        return localContext;
    }

    public static Cookie createApacheCookie(String value) {
        BasicClientCookie cookie = new BasicClientCookie(COOKIE_PARAM, value);
        cookie.setAttribute(BasicClientCookie.MAX_AGE_ATTR, COOKIE_LIFETIME);
        cookie.setSecure(true);
        return cookie;
    }

}
