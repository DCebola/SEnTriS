package pt.fct.nova.id.srv.application.clients;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;


public class TriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("TRIPLESTORE_DELETE_SOME_URI");
    private static final String QUERY_URI = System.getenv("TRIPLESTORE_QUERY_URI");
    private static final String FETCH_SCHEMA = System.getenv("TRIPLESTORE_FETCH_SCHEMA_URI");
    private static final String FETCH_INFO = System.getenv("TRIPLESTORE_FETCH_INFO_URI");

    public static CloseableHttpResponse fetchSchema(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(FETCH_SCHEMA, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse fetchInfo(CloseableHttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendGETRequest(httpClient, String.format(FETCH_INFO, triplestoreID), accessToken);

    }

    public static CloseableHttpResponse upload(HttpClient httpClient, String triplestoreID, Set<Triple> triples, boolean schema, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendPOSTRequest(httpClient, new URIBuilder(String.format(UPLOAD_URI, triplestoreID))
                .addParameter("schema", String.valueOf(schema)).build(), ParsingUtils.triplesSetToHttpEntity(triples), accessToken);
    }

    public static CloseableHttpResponse query(HttpClient httpClient, String triplestoreID, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(QUERY_URI, triplestoreID), ParsingUtils.queryExecutionPlanToHttpEntity(plan), accessToken);
    }

    public static CloseableHttpResponse deleteAll(HttpClient httpClient, String triplestoreID, boolean schema, String accessToken) throws IOException, URISyntaxException {
        return HTTPUtils.sendDELETERequest(httpClient, new URIBuilder(String.format(DELETE_ALL_URI, triplestoreID))
                .addParameter("schema", String.valueOf(schema)).build(), accessToken);
    }

    public static CloseableHttpResponse deleteSome(HttpClient httpClient, String triplestoreID, Set<Triple> triples, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(DELETE_SOME_URI, triplestoreID), ParsingUtils.triplesSetToHttpEntity(triples), accessToken);
    }


}
