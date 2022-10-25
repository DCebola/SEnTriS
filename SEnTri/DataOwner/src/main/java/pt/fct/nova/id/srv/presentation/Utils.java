package pt.fct.nova.id.srv.presentation;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import pt.fct.nova.id.srv.presentation.api.dtos.UploadForm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class.getName());

    private static Gson gson = new Gson();

    public static HttpEntity uploadFormToHttpEntity(UploadForm form) {
        return MultipartEntityBuilder
                .create()
                .addTextBody("syntax", form.getSyntax(), ContentType.create(MediaType.TEXT_PLAIN))
                .addTextBody("namespaces", gson.toJson(form.getNamespaces()), ContentType.create(MediaType.APPLICATION_JSON))
                .addBinaryBody("contents", form.getContents())
                .build();
    }

    public static Response buildResponse(CloseableHttpResponse response) {
        try {
            return Response.ok(new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8))
                    .status(response.getStatusLine().getStatusCode()).build();
        } catch (IOException e) {
            return Response.ok().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
