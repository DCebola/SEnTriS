package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.List;

public class ProxyClient {
    private static final String SAVE_BINDINGS_URI = System.getenv("PROXY_SAVE_BINDINGS");
    public static CloseableHttpResponse saveBindings(CloseableHttpClient httpClient, List<String> bindings, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, SAVE_BINDINGS_URI, ParsingUtils.bindingsToHttpEntity(bindings), accessToken);
    }
}
