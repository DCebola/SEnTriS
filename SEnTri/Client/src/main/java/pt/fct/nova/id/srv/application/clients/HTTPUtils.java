package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static pt.fct.nova.id.srv.presentation.controllers.ParsingUtils.*;

public class HTTPUtils {
    private static final String BEARER = "Bearer ";
    private static final String DOMAIN = System.getenv("DOMAIN");
    private static final String COOKIE_FORMAT = COOKIE_PARAM.concat("=%s; Path=/; Domain=").concat(DOMAIN).concat("; Secure; HttpOnly;");

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, String uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendDELETERequest(HttpClient httpClient, Cookie cookie, String uri) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, Cookie cookie, String uri) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, String uri, HttpEntity body) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(body);
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPUTRequest(HttpClient httpClient, Cookie cookie, String uri, HttpEntity body) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setEntity(body);
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, Cookie cookie, String uri, HttpEntity body) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setEntity(body);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendDELETERequest(HttpClient httpClient, Cookie cookie, String uri, String accessToken) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPUTRequest(HttpClient httpClient, Cookie cookie, String uri, String accessToken) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, Cookie cookie, String uri, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, URI uri) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }


    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendDELETERequest(HttpClient httpClient, Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, Cookie cookie, URI uri) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPUTRequest(HttpClient httpClient, Cookie cookie, URI uri, String accessToken) throws IOException {
        HttpPut request = new HttpPut(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setHeader(HttpHeaders.COOKIE, buildCookieHeader(cookie));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendPOSTRequest(HttpClient httpClient, String uri, HttpEntity body, String accessToken) throws IOException {
        HttpPost request = new HttpPost(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        request.setEntity(body);
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendGETRequest(HttpClient httpClient, String uri, String accessToken) throws IOException {
        HttpGet request = new HttpGet(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static CloseableHttpResponse sendDELETERequest(HttpClient httpClient, String uri, String accessToken) throws IOException {
        HttpDelete request = new HttpDelete(uri);
        request.setHeader(HttpHeaders.AUTHORIZATION, BEARER.concat(accessToken));
        return (CloseableHttpResponse) httpClient.execute(request);
    }

    public static String consumeResponseEntity(CloseableHttpResponse response) throws IOException {
        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        EntityUtils.consume(response.getEntity());
        return responseBody;
    }

    public static NewCookie extractCookie(CloseableHttpResponse response) {
        Header[] headers = response.getHeaders("Set-Cookie");
        if (headers.length > 0)
            return buildCookie(headers[0].getValue().split(";")[0].split("=")[1].replace("\"", ""));
        return null;
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
