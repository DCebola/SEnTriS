package pt.fct.nova.id.srv.application.clients;


import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;

public class ProxyClient {
    private static final String QUERY_URI = System.getenv("PROXY_QUERY_URI");

    public static CloseableHttpResponse query(HttpClient httpClient, String protocolVersion, byte[] keyBytes, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(QUERY_URI, protocolVersion), ParsingUtils.generateSecureQueryRequest(keyBytes, plan), accessToken);
    }

}
