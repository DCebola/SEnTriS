package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ProxyClient {
    private static final String QUERY_URI = System.getenv("PROXY_QUERY_URI");
    private static final String TEST_VALUES_URI = System.getenv("PROXY_TEST_VALUES_URI");

    public static CloseableHttpResponse query(HttpClient httpClient, SecretKey key, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, QUERY_URI, ParsingUtils.generateSecureQueryRequest(key, plan), accessToken);
    }

    public static CloseableHttpResponse testValues(HttpClient httpClient, String searchID, Set<String> values, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(TEST_VALUES_URI, searchID), ParsingUtils.stringSetToHttpEntity(values), accessToken);
    }
}
