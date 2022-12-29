package pt.fct.nova.id.srv.application.clients;

import jakarta.ws.rs.core.Cookie;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.jena.graph.Triple;
import pt.fct.nova.id.srv.application.protocols.exceptions.InvalidNodeException;
import pt.fct.nova.id.srv.application.query.plans.DefaultQueryExecutionPlan;
import pt.fct.nova.id.srv.presentation.controllers.ParsingUtils;

import java.io.IOException;
import java.util.List;


public class TriplestoreClient {
    private static final String UPLOAD_URI = System.getenv("TRIPLESTORE_UPLOAD_URI");
    private static final String DELETE_ALL_URI = System.getenv("TRIPLESTORE_DELETE_ALL_URI");
    private static final String DELETE_SOME_URI = System.getenv("TRIPLESTORE_DELETE_SOME_URI");
    private static final String QUERY_URI = System.getenv("TRIPLESTORE_QUERY_URI");

    public static CloseableHttpResponse upload(HttpClient httpClient, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(UPLOAD_URI, triplestoreID), ParsingUtils.triplesListToHttpEntity(triples), accessToken);
    }

    public static CloseableHttpResponse query(HttpClient httpClient, String triplestoreID, DefaultQueryExecutionPlan plan, String accessToken) throws IOException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(QUERY_URI, triplestoreID), ParsingUtils.queryExecutionPlanToHttpEntity(plan), accessToken);
    }

    public static CloseableHttpResponse deleteAll(HttpClient httpClient, String triplestoreID, String accessToken) throws IOException {
        return HTTPUtils.sendDELETERequest(httpClient, String.format(DELETE_ALL_URI, triplestoreID), accessToken);
    }

    public static CloseableHttpResponse deleteSome(HttpClient httpClient, String triplestoreID, List<Triple> triples, String accessToken) throws IOException, InvalidNodeException {
        return HTTPUtils.sendPOSTRequest(httpClient, String.format(DELETE_SOME_URI, triplestoreID), ParsingUtils.triplesListToHttpEntity(triples), accessToken);
    }

}
