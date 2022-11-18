package pt.fct.nova.id.srv.presentation;

import com.google.gson.Gson;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.fct.nova.id.srv.application.clients.iam.IAMStore;

import java.io.IOException;

public class Utils {

    private static final String NO_SESSION = "No valid session for user";
    private static final String WRONG_USER_OR_COOKIE = "Wrong cookie or User for the specified cookie";
    private static final String INVALID_COOKIE = "Invalid cookie";

    private static Logger logger = LoggerFactory.getLogger(Utils.class);
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
            return Response.ok(EntityUtils.toString(response.getEntity()))
                    .status(response.getStatusLine().getStatusCode()).build();
        } catch (IOException e) {
            return Response.ok().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

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
