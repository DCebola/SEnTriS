package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HTTPUtils {
    public static final String COOKIE_PARAM = "session";
    private static final String BEARER = "Bearer ";
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String COOKIE_FORMAT = COOKIE_PARAM.concat("=%s; Path=/; Domain=").concat(DOMAIN).concat("; Secure; HttpOnly;");

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static Response buildResponse(CloseableHttpResponse response) throws IOException {
        return Response.ok(consumeResponseEntity(response)).status(response.getStatusLine().getStatusCode()).build();
    }

    public static String consumeResponseEntity(CloseableHttpResponse response) throws IOException {
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        EntityUtils.consume(response.getEntity());
        return responseBody;
    }

    private static String buildCookieHeader(Cookie cookie) {
        return String.format(COOKIE_FORMAT, cookie.getValue());
    }


    public static String extractAccessToken(List<String> authorizationHeaders) {
        for (String val : authorizationHeaders) {
            if (val.contains(BEARER))
                return val.split(" ")[1];
        }
        return null;
    }
}
