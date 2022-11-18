package pt.fct.nova.id.srv.presentation;

import jakarta.ws.rs.core.Response;
import org.apache.http.client.methods.*;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Utils {

    public static Response buildResponse(CloseableHttpResponse response) throws IOException {
        return Response.ok(EntityUtils.toString(response.getEntity())).status(response.getStatusLine().getStatusCode()).build();
    }

}
