package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HTTPResponse {
    private final Response response;
    private final Response.Status status;
    private final String body;

    public HTTPResponse(NewCookie cookie, CloseableHttpResponse response) throws IOException {
        this.status = Response.Status.fromStatusCode(response.getStatusLine().getStatusCode());
        this.body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        this.response =  Response.ok(body)
                .status(status)
                .cookie(cookie)
                .build();
    }

    public HTTPResponse(CloseableHttpResponse response) throws IOException {
        this.status = Response.Status.fromStatusCode(response.getStatusLine().getStatusCode());
        this.body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        this.response =  Response.ok(body)
                .status(status)
                .build();
    }

    public Response build() {
        return response;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
