package pt.fct.nova.id.srv.application.clients;


import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;

public class ProxyClient {
    private static final String QUERY_URI = System.getenv("PROXY_QUERY_URI");

    public static CloseableHttpResponse query(HttpClient httpClient, String protocolVersion, byte[] keyBytes, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        System.out.printf((QUERY_URI) + "POST %n", protocolVersion);
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(QUERY_URI, protocolVersion), ParsingUtils.generateSecureQueryRequest(keyBytes, plan), accessToken);
    }

}
