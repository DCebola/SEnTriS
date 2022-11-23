package pt.fct.nova.id.srv.application.clients;

import com.github.jsonldjava.shaded.com.google.common.io.ByteSource;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class HttpUtils {

    public static final String COOKIE_PARAM = "session";
    private static final String COOKIE_LIFETIME = System.getenv("COOKIE_LIFETIME");
    private static final String BEARER = "Bearer ";

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }

    public static CloseableHttpResponse sendDELETERequest(Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri, HttpEntity body) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }
    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }

    public static CloseableHttpResponse sendDELETERequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri, HttpEntity body, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request, generateContext(cookie.getValue()));
        }
    }


    public static Response buildResponse(CloseableHttpResponse response) {
        try {
            return Response.ok(EntityUtils.toString(response.getEntity()))
                    .status(response.getStatusLine().getStatusCode()).build();
        } catch (IOException e) {
            return Response.ok().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static HttpContext generateContext(String cookieValue) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpContext localContext = new BasicHttpContext();
        cookieStore.addCookie(createApacheCookie(cookieValue));
        localContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        return localContext;
    }

    private static org.apache.http.cookie.Cookie createApacheCookie(String value) {
        BasicClientCookie cookie = new BasicClientCookie(COOKIE_PARAM, value);
        cookie.setAttribute(BasicClientCookie.MAX_AGE_ATTR, COOKIE_LIFETIME);
        cookie.setSecure(true);
        return cookie;
    }
}
