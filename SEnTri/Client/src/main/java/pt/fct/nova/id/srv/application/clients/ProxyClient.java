package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import javax.crypto.SecretKey;
import java.io.IOException;

public class ProxyClient {
    private static final String QUERY_URI = System.getenv("PROXY_QUERY_URI");

    public static CloseableHttpResponse query(HttpClient httpClient, SecretKey key, DefaultQueryExecutionPlan plan) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, QUERY_URI, ParsingUtils.generateSecureQueryRequest(key, plan));
    }
}
