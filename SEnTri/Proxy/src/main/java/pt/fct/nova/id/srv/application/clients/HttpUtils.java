package pt.fct.nova.id.srv.application.clients;

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
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpUtils {
    public static final String COOKIE_PARAM = "session";
    private static final String BEARER = "Bearer ";
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String COOKIE_FORMAT = COOKIE_PARAM.concat("=%s; Path=/; Domain=").concat(DOMAIN).concat("; Secure; HttpOnly;");

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPSClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static Response buildResponse(CloseableHttpResponse response) throws IOException {
        return Response.ok(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                .status(response.getStatusLine().getStatusCode()).build();
    }

    private static String buildCookieHeader(Cookie cookie) {
        return String.format(COOKIE_FORMAT, cookie.getValue());
    }

    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            if (val.contains("Bearer"))
                return val;
        }
        return null;
    }
}
