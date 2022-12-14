package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;

public class HTTPUtils {
    private static final String BEARER = "Bearer ";
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String COOKIE_FORMAT = COOKIE_PARAM.concat("=%s; Path=/; Domain=").concat(DOMAIN).concat("; Secure; HttpOnly;");

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendDELETERequest(Cookie cookie, String uri) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(String uri, HttpEntity body) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri, HttpEntity body) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(body);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPUTRequest(Cookie cookie, String uri, HttpEntity body) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setEntity(body);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendDELETERequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPUTRequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, String uri, HttpEntity body, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setEntity(body);
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendGETRequest(Cookie cookie, URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }


    public static CloseableHttpResponse sendGETRequest(Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendDELETERequest(Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }


    public static CloseableHttpResponse sendPOSTRequest(Cookie cookie, URI uri) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static CloseableHttpResponse sendPUTRequest(Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        try (CloseableHttpClient client = HTTPClient.buildClient()) {
            return client.execute(request);
        }
    }

    public static Response buildResponse(String prefix, CloseableHttpResponse response) throws IOException {
        return Response.ok(prefix.concat(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)))
                .status(response.getStatusLine().getStatusCode()).build();
    }

    public static Response buildResponse(CloseableHttpResponse response) throws IOException {
        Header[] headers = response.getHeaders("Set-Cookie");
        if (headers.length > 0)
            return Response.ok(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .cookie(buildCookie(headers[0].getValue().split(";")[0].split("=")[1].replace("\"", "")))
                    .status(response.getStatusLine().getStatusCode()).build();
        return Response.ok(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)).status(response.getStatusLine().getStatusCode()).build();
    }


    public static NewCookie buildCookie(String uid) {
        return new NewCookie.Builder(COOKIE_PARAM)
                .value(uid)
                .path("/")
                .secure(true)
                .httpOnly(true)
                .build();
    }

    private static String buildCookieHeader(Cookie cookie) {
        return String.format(COOKIE_FORMAT, cookie.getValue());
    }


}
